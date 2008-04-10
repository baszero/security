package org.wyona.security.impl;

import java.util.Hashtable;

import org.wyona.commons.io.Path;
import org.wyona.commons.io.PathUtil;
import org.wyona.security.core.AuthorizationException;
import org.wyona.security.core.api.Identity;
import org.wyona.security.core.api.Policy;
import org.wyona.security.core.api.PolicyManager;
import org.wyona.security.core.api.Role;
import org.wyona.security.core.api.Usecase;
import org.wyona.yarep.core.NoSuchNodeException;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryFactory;
import org.wyona.yarep.util.RepoPath;
import org.wyona.yarep.util.YarepUtil;

import org.apache.log4j.Logger;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

/**
 *
 */
public class PolicyManagerImplVersion2 implements PolicyManager {

    private static Logger log = Logger.getLogger(PolicyManagerImplVersion2.class);

    private Repository policiesRepository;
    private DefaultConfigurationBuilder configBuilder;

    private static String USECASE_ELEMENT_NAME = "usecase";

    /**
     *
     */
    public PolicyManagerImplVersion2(Repository policiesRepository) {
        this.policiesRepository = policiesRepository;
        configBuilder = new DefaultConfigurationBuilder();
    }
    
    /**
     * Get policies repository
     */
     public Repository getPoliciesRepository() {
           return policiesRepository;
     }
         
     
    /**
     * @deprecated
     */
    public boolean authorize(Path path, Identity identity, Role role) throws AuthorizationException {
        return authorize(path.toString(), identity, role);
       
    }

    /**
     * @deprecated
     */
    public boolean authorize(String path, Identity identity, Role role) throws AuthorizationException {
        log.warn("Deprecated method and not implemented!");
        return false;
    }

    /**
     *
     */
    public boolean authorize(Policy policy, Identity identity, Usecase usecase) throws AuthorizationException {
        log.error("Not implemented yet!");
        return false;
    }
   
    /**
     *
     */
    public boolean authorize(String path, Identity identity, Usecase usecase) throws AuthorizationException {
        if(path == null || identity == null || usecase == null) {
            log.error("Path or identity or usecase is null! [" + path + ", " + identity + ", " + usecase + "]");
            throw new AuthorizationException("Path or identity or usecase is null! [" + path + ", " + identity + ", " + usecase + "]");
        }

        try {
            return authorize(policiesRepository, path, identity, usecase);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthorizationException("Error authorizing " + policiesRepository.getID() + 
                    ", " + path + ", " + identity + ", " + usecase, e);
        }
    }

    /**
     *
     */
    private boolean authorize(Repository repo, String path, Identity identity, Usecase usecase) throws Exception {
        if(repo == null) {
            log.error("Repo is null!");
            throw new Exception("Repo is null!");
        } else if(path == null) {
            log.error("Path is null!");
            throw new Exception("Path is null!");
        } else if(identity == null) {
            log.error("Identity is null!");
            throw new Exception("Identity is null!");
        } else if(usecase == null) {
            log.error("Usecase is null!");
            throw new Exception("Usecase is null!");
        }

        String yarepPath = getPolicyPath(path); 
        log.debug("Policy Yarep Path: " + yarepPath + ", Original Path: " + path + ", Repo: " + repo);
        if (repo.existsNode(yarepPath)) {
            try {
                Configuration config = configBuilder.build(repo.getNode(yarepPath).getInputStream());
                boolean useInheritedPolicies = config.getAttributeAsBoolean("use-inherited-policies", true);

            Configuration[] usecases = config.getChildren(USECASE_ELEMENT_NAME);
            for (int i = 0; i < usecases.length; i++) {
                String usecaseName = usecases[i].getAttribute("id", null);
                if (usecaseName != null && usecaseName.equals(usecase.getName())) {
                    boolean useInheritedRolePolicies = usecases[i].getAttributeAsBoolean("use-inherited-policies", true);
                    Configuration[] accreditableObjects = usecases[i].getChildren();

                    boolean worldCredentialExists = false;
                    boolean worldIsNotAuthorized = true;
                    for (int k = 0; k < accreditableObjects.length; k++) {
                        String aObjectName = accreditableObjects[k].getName();
                        log.debug("Accreditable Object Name: " + aObjectName);

                        if (aObjectName.equals("world")) {
                            worldCredentialExists = true;
                            String permission = accreditableObjects[k].getAttribute("permission", null);
                            if (permission.equals("true")) {
                                log.debug("Access granted: " + path);
                                worldIsNotAuthorized = false;
                                return true;
                            } else {
                                worldIsNotAuthorized = true;
                            }
                        } else if (aObjectName.equals("group")) {
                            if (identity.getGroupnames() != null) {
                                String groupName = accreditableObjects[k].getAttribute("id", null);
                                String[] groupnames = identity.getGroupnames();
                                if (groupnames != null) {
                                    for (int j = 0; j < groupnames.length; j++) {
                                        if (groupName.equals(groupnames[j])) {
                                            String permission = accreditableObjects[k].getAttribute("permission", null);
                                            if (permission.equals("true")) {
                                                log.debug("Access granted: Path = " + path + ", Group = " + groupName);
                                                return true;
                                            } else {
                                                log.debug("Access denied: Path = " + path + ", Group = " + groupName);
                                                return false;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (aObjectName.equals("user")) {
                            if (identity.getUsername() != null) {
                                String userName = accreditableObjects[k].getAttribute("id", null);
                                if (userName.equals(identity.getUsername())) {
                                    String permission = accreditableObjects[k].getAttribute("permission", null);
                                    if (permission.equals("true")) {
                                        log.debug("Access granted: Path = " + path + ", User = " + userName);
                                        return true;
                                    } else {
                                        log.debug("Access denied: Path = " + path + ", User = " + userName);
                                        return false;
                                    }
                                }
                            }
                        } else if (aObjectName.equals("iprange")) {
                            log.warn("Credential IP Range not implemented yet!");
                            //return false;
                        } else {
                            log.warn("No such accreditable object implemented: " + aObjectName);
                            //return false;
                        }
                    }
                    if (worldCredentialExists && worldIsNotAuthorized) {
                       log.debug("Access for world denied: " + path);
                       return false;
                    }
                    if (!useInheritedRolePolicies){
                        log.debug("Policy inheritance disabled for usecase:" + usecaseName + ". Access denied: "+ path);
                        return false;
                    }
                }
            }
                if (!useInheritedPolicies) {
                    log.debug("Policy inheritance disabled. Access denied: "+ path);
                    return false;
                }
            } catch(NoSuchNodeException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            if (yarepPath.equals("/.policy")) {
                log.warn("No such node: " + yarepPath + " (" + repo + ")");
            } else {
                if (log.isDebugEnabled()) log.debug("No such node: " + yarepPath + " (Fallback to parent policy ...)");
            }
        }

        String parent = PathUtil.getParent(path);
        if (parent != null) {
            // Check policy of parent in order to inherit credentials ...
            log.debug("Check parent policy: " + parent + " ... (Current path: " + path + ")");
            return authorize(repo, parent, identity, usecase);
        } else {
            log.warn("Trying to get parent of " + path + " (" + repo + ") failed. Access denied.");
            return false;
        }
    }

    /**
     * Append '.policy' to path as suffix
     */
    private String getPolicyPath(String path) {
        // Remove trailing slash except for ROOT ...
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1) + ".policy";
        }
        return path + ".policy";
    }

    /**
     *
     */
    public Policy getPolicy(String path, boolean aggregate) throws AuthorizationException {
        try {
            if (aggregate) {
                return org.wyona.security.impl.util.PolicyAggregator.aggregatePolicy(path, this);
            } else {
                if (getPoliciesRepository().existsNode(getPolicyPath(path))) {
                    return new PolicyImplV2(getPoliciesRepository().getNode(getPolicyPath(path)).getInputStream());
                } else {
                    if (!path.equals("/")) {
                        log.warn("No policy found for '" + path + "' (Policies Repository: " + getPoliciesRepository().getName() + "). Check for parent '" + PathUtil.getParent(path) + "'.");
                        return null;
                        //return getPolicy(PathUtil.getParent(path), false);
                    } else {
                        log.warn("No policies found at all, not even a root policy!");
                        return null;
                    }
                }
            }
        } catch(Exception e) {
            log.error(e, e);
            throw new AuthorizationException(e.getMessage());
        }
    }

    /**
     * @see
     */
    public void setPolicy(String path, Policy policy) {
        log.warn("TODO: Not implemented yet!");
    }

    /**
     * @see
     */
    public String[] getUsecases() {
        log.warn("TODO: Implementation not finished yet! Read from configuration instead hardcoded!");
        String[] usecases = {"view", "open", "write", "resource.create", "delete", "introspection", "toolbar", "policy.read", "policy.update"};
        return usecases;
    }

    /**
     * @see
     */
    public String getUsecaseLabel(String usecaseId, String language) {
        log.warn("TODO: Implementation not finished yet! Read from configuration instead hardcoded!");
        if (language.equals("de")) {
            if (usecaseId.equals("view")) {
                return "Anschauen/Lesen";
            } else if (usecaseId.equals("open")) {
                return "Open content for editing";
            } else {
                return "No label for \"" + usecaseId + "\"";
            }
        } else {
            if (usecaseId.equals("view")) {
                return "View/Read";
            } else if (usecaseId.equals("open")) {
                return "Open content for editing";
            } else if (usecaseId.equals("write")) {
                return "Write/Save";
            } else {
                return "No label for \"" + usecaseId + "\"";
            }
        }
    }
    
    public Policy createEmptyPolicy() throws AuthorizationException {
        try {
            return new PolicyImplV2();
        } catch (Exception e) {
            throw new AuthorizationException(e.getMessage(), e);
        }
    }

}
