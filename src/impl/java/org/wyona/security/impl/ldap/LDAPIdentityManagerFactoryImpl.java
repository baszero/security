package org.wyona.security.impl.ldap;

import org.wyona.security.core.IdentityManagerFactory;
import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.IdentityManager;
import org.wyona.yarep.core.Repository;

import org.apache.log4j.Logger;

/**
 * Factory in order to get LDAP implementation
 */
public class LDAPIdentityManagerFactoryImpl extends IdentityManagerFactory {

    private static Logger log = Logger.getLogger(LDAPIdentityManagerFactoryImpl.class);

    /**
     * @see org.wyona.security.core.IdentityManagerFactory#newIdentityManager(Repository)
     */
    public IdentityManager newIdentityManager(Repository identitiesRepository) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("This operation should never be called");
    }

    /**
     * @see org.wyona.security.core.IdentityManagerFactory#newIdentityManager(Document, URIResolver)
     */
    public IdentityManager newIdentityManager(org.w3c.dom.Document configuration, javax.xml.transform.URIResolver resolver) {
        try {
            boolean load = false;
/*
            org.wyona.security.impl.ldap.LDAPClient ldapClient = new org.wyona.security.impl.ldap.LDAPClientImplV2();
            ldapClient.setProviderURL("ldap://127.0.0.1:10389");
*/
            org.wyona.security.impl.ldap.LDAPClient ldapClient = new org.wyona.security.impl.ldap.LDAPClientImpl();
            ldapClient.setProviderURL("ldap://192.168.200.109:389");

            ldapClient.setAuthenticationMechanism("simple");
            //ldapClient.setSecurityProtocol("ssl");

            return new org.wyona.security.impl.ldap.LDAPIdentityManagerImpl(getRepository(configuration, resolver), load, ldapClient);
        } catch (AccessManagementException e) {
            log.error(e, e);
        } catch (Exception e) {
            log.error(e, e);
        }
        return null;
    }

    /**
     * Get Yarep repository
     * @param configuration XML configuration, e.g. <identity-manager-config xmlns="http://www.wyona.org/security/1.0">config/ac-identities-repository.xml</identity-manager-config>
     */
    private Repository getRepository(org.w3c.dom.Document configuration, javax.xml.transform.URIResolver resolver) throws Exception {
        log.debug("Configuration: " + org.wyona.commons.xml.XMLHelper.documentToString(configuration, false, true, null));
        String xpath = "/*[local-name()='identity-manager-config']/*[local-name()='config']/*[local-name()='yarep-repo']";
        String[] relativeRepoPath = org.wyona.commons.xml.XMLHelper.valueOf(configuration, xpath);
        if(relativeRepoPath != null && relativeRepoPath.length > 0) {
            log.debug("Relative repository path: " + relativeRepoPath[0]);
        } else {
            throw new Exception("No repo path found within configuration: " + org.wyona.commons.xml.XMLHelper.documentToString(configuration, false, true, null) + ", " + xpath);
        }
        java.io.File repoConfigFile = new java.io.File(resolver.resolve(relativeRepoPath[0], null).getSystemId());
        if (repoConfigFile.isFile()) {
            log.debug("Repository configuration file: " + repoConfigFile.getAbsolutePath());
            return new org.wyona.yarep.core.RepositoryFactory().newRepository("identities", repoConfigFile);
        } else {
            throw new Exception("No such file or directory: " + repoConfigFile.getAbsolutePath());
        }
    }
}
