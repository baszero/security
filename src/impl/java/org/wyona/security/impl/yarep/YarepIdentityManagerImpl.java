package org.wyona.security.impl.yarep;

import java.util.Hashtable;

import org.wyona.security.core.AuthenticationException;
import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.GroupManager;
import org.wyona.security.core.api.Identity;
import org.wyona.security.core.api.IdentityManager;
import org.wyona.security.core.api.UserManager;
import org.wyona.security.impl.yarep.YarepGroupManager;
import org.wyona.security.impl.yarep.YarepUserManager;
import org.wyona.yarep.core.Path;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryFactory;
import org.wyona.yarep.util.RepoPath;
import org.wyona.yarep.util.YarepUtil;

import org.apache.log4j.Category;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

/**
 *
 */
public class YarepIdentityManagerImpl implements IdentityManager {

    private static Category log = Category.getInstance(YarepIdentityManagerImpl.class);

    private Repository identitiesRepository;
    private DefaultConfigurationBuilder configBuilder;
    private UserManager userManager;
    private GroupManager groupManager;

    private static String CONFIG= "ac-identities-yarep.properties";

    /**
     *
     */
    public YarepIdentityManagerImpl(Repository identitiesRepository) {
        this.identitiesRepository = identitiesRepository;
        configBuilder = new DefaultConfigurationBuilder(true);
        try {
            // TODO: make implementation configurable:
            userManager = new YarepUserManager(this, this.identitiesRepository);
            groupManager = new YarepGroupManager(this, this.identitiesRepository);
        } catch (AccessManagementException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e); // FIXME
        }
    }

    
    /**
     * @deprecated
     */
    public boolean authenticate(String username, String password) throws AuthenticationException {
        try {
            return this.userManager.getUser(username).authenticate(password);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationException(e);
        }

/*
        if(username == null || password == null) {
            log.warn("Username or password is null!");
            return false;
        }

        log.debug("Repository: " + identitiesRepository);

        try {
            Configuration config = configBuilder.build(identitiesRepository.getInputStream(new Path("/" + username + ".iml")));
            
            if (config.getChild("salt", false) == null) {         	
                Configuration passwdConfig = config.getChild("password");
                if (passwdConfig.getValue().equals(Password.getMD5(password))) return true;
            } else  {        	        	
                Configuration passwdConfig = config.getChild("password");
                String salt = config.getChild("salt").getValue();
                if(passwdConfig.getValue().equals(Password.getMD5(password,salt))) return true;
            } 
            
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthenticationException("Error authenticating " + identitiesRepository.getID() + ", " + username, e);
        }

        return false;
*/
    }


    public GroupManager getGroupManager() {
        return this.groupManager;
    }


    public UserManager getUserManager() {
        return this.userManager;
    }
}