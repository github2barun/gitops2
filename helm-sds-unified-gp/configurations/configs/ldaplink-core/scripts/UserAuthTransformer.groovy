import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import se.seamless.ers.extlink.ldap.request.ILdapRequest
import se.seamless.ers.extlink.ldap.request.UserAuthRequest
import se.seamless.ers.extlink.ldap.response.ILdapResponse
import se.seamless.ers.extlink.ldap.service.UserService
import se.seamless.ers.extlink.ldap.transformer.UserTransformer

/**
 * Created Date : 07/07/2021
 * Time         : 1:04 PM
 *
 * @author : Muhammad Noman Maraaj <muhammad.noman@seamless.se>
 * Purpose      :
 * <p>
 * Copyright(c) 2021. Seamless Distribution Systems AB - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited. It is proprietary and confidential.
 */

@Component("userAuthTransformer")
class UserAuthTransformer extends UserTransformer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserAuthTransformer.class);

    @Autowired
    private final UserService userService;

    @Override
    protected ILdapRequest transformInBoundRequest(ILdapRequest ldapRequest)
    {
        /*
          alter the request parameter if needed
         */
        LOGGER.atInfo().log("transforming inbound request");

        LOGGER.atInfo().log("transformed request: {}", ldapRequest);

        return ldapRequest;
    }

    @Override
    protected ILdapResponse transformOutBoundResponse(ILdapResponse ldapResponse) {

        /*
          add additional response data if required
         */
        LOGGER.atInfo().log("transforming outbound response");
        LOGGER.atInfo().log("transformed response: {}", ldapResponse);

        return ldapResponse;
    }

    @Override
    protected ResponseEntity<ILdapResponse> processRequest(ILdapRequest ldapRequest)
    {
        LOGGER.atInfo().log("processing the inbound request...");
        /*
          process the request accordingly
         */
        LOGGER.atInfo().log("process request: {}", ldapRequest);
        return userService.authenticateUser((UserAuthRequest) ldapRequest);
    }
}