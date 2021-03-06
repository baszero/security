package org.wyona.security.impl.ldap;

/**
 *
 */
public interface LDAPClient {

    /**
     * Set URL of LDAP server
     * @param url URL of LDAP server, e.g. ldap://127.0.0.1:389
     */
    public void setProviderURL(String url) throws Exception;

    /**
     * Set security authentication mechanism
     * @param am Security authentication mechanism, e.g. simple (also see http://java.sun.com/products/jndi/tutorial/ldap/security/ldap.html)
     */
    public void setAuthenticationMechanism(String am) throws Exception;

    /**
     * Set security protocol
     * @param protocol Security protocol, e.g. ssl
     */
    public void setSecurityProtocol(String protocol) throws Exception;

    /**
     * Get all usernames
     */
    public String[] getAllUsernames() throws Exception;
}
