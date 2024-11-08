/*
 * ************************************************************************
 * Copyright (c) 2021. Seamless Distribution Systems AB - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited. It is proprietary and confidential.
 * Written by Ahmed <muhammad.ahmed@seamless.se>
 * ****************************************************************************
 */

package com.seamless.ims.service.impl

import com.google.gson.Gson
import se.seamless.sfo.lib.exception.ValidationException
import com.seamless.ims.enums.ImportType
import com.seamless.ims.enums.ProductType
import com.seamless.ims.exception.BadDataException
import com.seamless.ims.helper.InventoryImportHelper
import com.seamless.ims.model.Product
import com.seamless.ims.model.request.BulkInventoryDto
import com.seamless.ims.model.request.BulkInventoryRequest
import com.seamless.ims.model.response.BulkImportResponse
import com.seamless.ims.model.response.ChunkProcessingInfo
import com.seamless.ims.model.response.Details
import com.seamless.ims.service.ProvisioningHandler
import com.seamless.ims.service.adapter.InventoryItemAdapter
import com.seamless.ims.service.adapter.ResponseAdapter
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import se.seamless.sfo.lib.exception.NotFoundException
import se.seamless.sfo.lib.model.IMSError

//@Component("generic")
class GenericProvisioningHandler implements ProvisioningHandler
{
    private final Logger log = LoggerFactory.getLogger(GenericProvisioningHandler.class);
    @Autowired
    InventoryImportHelper inventoryImportHelper;
    @Autowired
    InventoryItemAdapter inventoryItemAdapter;
    @Autowired
    private ResponseAdapter responseAdapter;
    @Autowired
    private Gson gson;

    @Override
    public BulkImportResponse handleImport(
            BulkInventoryRequest request, String token)
    {
        int processedRecord = 0;
        int passedRecords = 0;
        int recordNo = 0;
        boolean isSerialRangeException = false;
        boolean isNonSerialException = false;
        BulkImportResponse response = new BulkImportResponse();
        Map<String, String> invalidInventories = new HashMap<>();
        response.setChunkProcessingInfo(new ArrayList<>());

        long nullProductSkuCount  = request.getInventories().stream()
                .filter({ inventoryModel -> inventoryModel.getProductSKU() == null || inventoryModel.getProductSKU().isEmpty() })
                .count();
        if(nullProductSkuCount > 0){
            log.info("ProductSKU must be provided");
            throw new BadDataException("inventory.body.request.productSKU.invalid");
        }

        Map<String, Product> products = inventoryImportHelper.fetchProductTypeByProductSKUs(request);

        if(products.isEmpty()) {
            log.info("Could not find any products for given productSKU");
            throw new BadDataException("productid.not.valid");
        }

        String existingBatchId = inventoryImportHelper.getBatchIdsForTheImport(request);
        for (BulkInventoryDto inventoryModel : request.getInventories())
        {
            if (StringUtils.isBlank(inventoryModel.getBatchId()) && StringUtils.isNotBlank(existingBatchId)  && ProductType.SERIALIZED.equals(products.get(inventoryModel.getProductSKU()).getType()))
            {
                inventoryModel.setBatchId(existingBatchId);
            }
        }

        inventoryImportHelper.generateBoxIds(request.getInventories(),products);

        int totalRecords = request.getInventories().size();

        for (BulkInventoryDto inventoryModel : request.getInventories())
        {
            try
            {
                ++recordNo;
                if (ProductType.SERIALIZED.equals(products.get(inventoryModel.getProductSKU()).getType()))
                {
                    List<String> serials = new ArrayList<>();

                    if (inventoryModel.hasSerialNumber())
                    {
                        serials.add(inventoryModel.getSerialNo());
                    }
                    else
                    {
                        inventoryImportHelper.validateStartAndEndNumber(inventoryModel);
                        inventoryImportHelper.validateStartNumber(inventoryModel);
                        inventoryImportHelper.validateStartNumberNegativeValue(inventoryModel);
                        inventoryImportHelper.validateEndNumberNegativeValue(inventoryModel);

                        serials = inventoryModel.generateSerials();
                    }

                    if (serials.size() == 0)
                    {
                        throw new BadDataException(responseAdapter.getMessage("inventory.serialized.startNo.endNo.serialNo.null", null));
                    }

                    for (String serial : serials)
                    {
                        try
                        {
                            processedRecord++;
                            inventoryModel.setSerialNo(serial);
                            inventoryModel.setResourceId(request.getBatchId());
                            inventoryImportHelper.validateInventoryModel(inventoryModel, products);
                            if (ImportType.valueOf(request.getImportType()) != ImportType.FULL_IMPORT)
                                inventoryItemAdapter.createInventoryWithNewTransaction(inventoryModel);
                            else
                                inventoryItemAdapter.createInventory(inventoryModel);

                            passedRecords++;
                            response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(0).details(
                                    populateSuccessDetailsList()).build());
                            response.getSuccessfulRecords().add(inventoryImportHelper.convertToBulkTDRModel(
                                    inventoryModel,
                                    "Successfully Created."));

                        }
                        catch (Exception e)
                        {
                            log.error("Inventory Record [ " + inventoryModel + "]", e);
                            String error = e.getMessage();
                            if (e instanceof ValidationException || e instanceof NotFoundException)
                            {
                                error = gson.fromJson(e.getMessage(), IMSError.class).getMessage();
                            }
                            String message = responseAdapter.getMessage(error, null);
                            invalidInventories.put(StringUtils.isBlank(inventoryModel.getSerialNo()) ?
                                    inventoryModel.getBatchId() :
                                    inventoryModel.getSerialNo(), message);
                            response.getFailingRecords().add(inventoryImportHelper.convertToBulkTDRModel(inventoryModel, message));
                            response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(1234).details(
                                    populateErrorDetailList(message)).build());
                            if (ImportType.valueOf(request.getImportType()) == ImportType.FULL_IMPORT)
                            {
                                passedRecords = 0;
                                inventoryImportHelper.setResponseOnFuLLImport(request, processedRecord, response, totalRecords);
                                isSerialRangeException = true;
                                break;
                            }
                            else if (ImportType.valueOf(request.getImportType()) == ImportType.FAIL_ON_ERROR)
                            {
                                inventoryImportHelper.setResponseOnFailOnError(request, processedRecord, response, totalRecords);
                                isSerialRangeException = true;
                                break;
                            }
                            else if (ImportType.valueOf(request.getImportType()) == ImportType.SKIP_ON_ERROR)
                            {
                                inventoryImportHelper.setResponseOnSkipOnError(response);
                            }
                        }
                    }
                }
                else if (ProductType.NONSERIALIZED.equals(products.get(inventoryModel.getProductSKU()).getType()))
                {
                    try
                    {
                        processedRecord++;
                        inventoryModel.setResourceId(request.getBatchId());

                        inventoryImportHelper.validateInventoryModel(inventoryModel, products);

                        if (ImportType.valueOf(request.getImportType()) != ImportType.FULL_IMPORT)
                            inventoryItemAdapter.createInventoryWithNewTransaction(inventoryModel);
                        else
                            inventoryItemAdapter.createInventory(inventoryModel);

                        passedRecords++;
                        response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(0).details(
                                populateSuccessDetailsList()).build());

                    }
                    catch (Exception e)
                    {
                        log.error("Inventory Record [ " + inventoryModel + "]");
                        log.error("addInventories : ", e);
                        String error = e.getMessage();
                        if (e instanceof ValidationException || e instanceof NotFoundException)
                        {
                            error = gson.fromJson(e.getMessage(), IMSError.class).getMessage();
                        }
                        String message = responseAdapter.getMessage(error, null);
                        invalidInventories.put("Non serialised inventory", message);
                        response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(1234).details(
                                populateErrorDetailList(message)).build());
                        response.getFailingRecords().add(inventoryImportHelper.convertToBulkTDRModel(inventoryModel, message));

                        if (ImportType.valueOf(request.getImportType()) == ImportType.FULL_IMPORT)
                        {
                            passedRecords = 0;
                            inventoryImportHelper.setResponseOnFuLLImport(request, processedRecord, response, totalRecords);
                            isNonSerialException = true;
                            break;
                        }
                        else if (ImportType.valueOf(request.getImportType()) == ImportType.FAIL_ON_ERROR)
                        {
                            inventoryImportHelper.setResponseOnFailOnError(request, processedRecord, response, totalRecords);
                            isNonSerialException = true;
                            break;
                        }
                        else if (ImportType.valueOf(request.getImportType()) == ImportType.SKIP_ON_ERROR)
                        {
                            inventoryImportHelper.setResponseOnSkipOnError(response);
                        }
                    }
                }
                else
                {
                    processedRecord++;
                    inventoryModel.setResourceId(request.getBatchId());

                    inventoryImportHelper.validateInventoryModel(inventoryModel, products);

                    if (ImportType.valueOf(request.getImportType()) != ImportType.FULL_IMPORT)
                        inventoryItemAdapter.createInventoryWithNewTransaction(inventoryModel);
                    else
                        inventoryItemAdapter.createInventory(inventoryModel);

                    passedRecords++;
                    response.getSuccessfulRecords().add(inventoryImportHelper.convertToBulkTDRModel(
                            inventoryModel,
                            "Successfully Created."));
                    response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(0).details(
                            populateSuccessDetailsList()).build());
                }
                if (isSerialRangeException || isNonSerialException)
                    break;
            }
            catch (Exception e)
            {
                log.error("Inventory Record [ " + inventoryModel + "]");
                log.error("addInventories : ", e);
                String error = e.getMessage();
                if (e instanceof ValidationException || e instanceof NotFoundException)
                {
                    error = gson.fromJson(e.getMessage(), IMSError.class).getMessage();
                }
                String message = responseAdapter.getMessage(error, null);
                invalidInventories.put(StringUtils.isBlank(inventoryModel.getSerialNo()) ?
                        inventoryModel.getBatchId() :
                        inventoryModel.getSerialNo(), message);
                response.getFailingRecords().add(inventoryImportHelper.convertToBulkTDRModel(inventoryModel, message));
                response.getChunkProcessingInfo().add(ChunkProcessingInfo.builder().recordNumber(recordNo).status(1234).details(
                        populateErrorDetailList(message)).build());
                if (ImportType.valueOf(request.getImportType()) == ImportType.FULL_IMPORT)
                {
                    passedRecords = 0;
                    inventoryImportHelper.setResponseOnFuLLImport(request, processedRecord, response, totalRecords);
                    break;
                }
                else if (ImportType.valueOf(request.getImportType()) == ImportType.FAIL_ON_ERROR)
                {
                    inventoryImportHelper.setResponseOnFailOnError(request, processedRecord, response, totalRecords);

                    break;
                }
                else if (ImportType.valueOf(request.getImportType()) == ImportType.SKIP_ON_ERROR)
                {
                    inventoryImportHelper.setResponseOnSkipOnError(response);
                }
            }
        }

        if (response.getFailingRecords().isEmpty())
        {
            inventoryImportHelper.setResponseOnSuccess(response);
        }
        response.setErsReference(inventoryImportHelper.getSystemToken(token).getErsReference());
        response.setPassedRecords(passedRecords);
        response.setProcessedRecords(processedRecord);
        response.setFailedRecords(response.getFailingRecords().size());
        response.setImportId(request.getBatchId());
        response.setImportType(request.getImportType());
        response.setResultCode(inventoryImportHelper.getConfiguredCode(passedRecords == totalRecords ? "SUCCESS" : request.getImportType()));
        response.setResultDetails(invalidInventories);
        return response;
    }

    List<Details> populateErrorDetailList(String errMsg)
    {
        Details errCodeDetails = new Details();
        Details errMsgDetails = new Details();
        List<Details> detailsList = new ArrayList<>();
        errCodeDetails.setKey("errCode");
        errCodeDetails.setValue("1234");
        errMsgDetails.setKey("errMessage");
        errMsgDetails.setValue(errMsg);
        detailsList.add(errCodeDetails);
        detailsList.add(errMsgDetails);
        return detailsList;
    }

    List<Details> populateSuccessDetailsList()
    {
        List<Details> detailsList = new ArrayList<>();
        Details errCodeDetails = new Details();
        errCodeDetails.setKey("errCode");
        errCodeDetails.setValue("0");
        detailsList.add(errCodeDetails);
        return detailsList;
    }
}