package se.seamless.ers.extlink.ldap.service

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.springframework.stereotype.Component
import se.seamless.ers.extlink.ldap.model.User
import se.seamless.ers.extlink.ldap.transformer.AttributesMapTransformer

import javax.naming.directory.Attributes

@Component
class UserAttributesMapping extends AttributesMapTransformer
{
    private static final Logger LOGGER = LogManager.getLogger(UserAttributesMapping.class);
    @Override
    User mapUserAttributes(Attributes attributes, String userDn) {
        LOGGER.atInfo().log("UserAttributesMapping Attribute Size: {}", attributes.size());
        LOGGER.atInfo().log("UserAttributesMapping Attribute : {}", attributes.getAll());
        User user = new User();
        if (null != attributes.get("cn")) {
            user.setFullName((String) attributes.get("cn").get());
        }
        if (null != attributes.get("sn")) {
            user.setLastName((String) attributes.get("sn").get());
        }
        if (null != attributes.get("sAMAccountName")) {
            user.setUsername((String) attributes.get("sAMAccountName").get());
        }
        if (null != attributes.get("memberOf")) {
            user.setMemberOfGroup((String) attributes.get("memberOf").get());
        }
        if (null != attributes.get("mail")) {
            user.setEmailAddress((String) attributes.get("mail").get());
        }
        if (null != attributes.get("telephoneNumber")) {
            user.setPhoneNumber((String) attributes.get("telephoneNumber").get());
        }
        user.setUserDn(userDn);
        return user;
    }
}