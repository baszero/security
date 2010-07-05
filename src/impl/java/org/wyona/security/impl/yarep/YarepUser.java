package org.wyona.security.impl.yarep;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;

import org.apache.log4j.Logger;

import org.wyona.security.core.ExpiredIdentityException;
import org.wyona.security.core.UserHistory;
import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.Group;
import org.wyona.security.core.api.GroupManager;
import org.wyona.security.core.api.Item;
import org.wyona.security.core.api.UserManager;
import org.wyona.security.core.api.User;
import org.wyona.security.impl.Password;

import org.wyona.yarep.core.Node;

/**
 * User implementation based on Yarep
 */
public class YarepUser extends YarepItem implements User {

    private static Logger log = Logger.getLogger(YarepUser.class);

    public static final String USER = "user";

    private boolean fixGroupIndex = true;
    public static final String GROUPS_TAG_NAME = "groups";
    public static final String GROUP_TAG_NAME = "group";
    public static final String GROUP_ID_ATTR_NAME = "id";

    public static final String EMAIL = "email";

    public static final String LANGUAGE = "language";

    public static final String PASSWORD = "password";

    public static final String SALT = "salt";
        
    public static final String EXPIRE = "expire";
    
    public static final String DESCRIPTION = "description";

    /**
     * Date format used for the expired value
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    private String email;

    private String language;

    private String encryptedPassword;

    private String salt;
    
    private String description;
    
    private Date expire;

    private ArrayList _groupIDs;

    /**
     * Instantiates an existing YarepUser from a repository node.
     *
     * @param userManager
     * @param groupManager
     * @param node
     */
    public YarepUser(UserManager userManager, GroupManager groupManager, Node node) throws AccessManagementException {
        super(userManager, groupManager, node); // INFO: This will call configure()
        //log.debug("User has been initialized.");
    }
    
    /**
     * Creates a new YarepUser with a given id and name (not persistent)
     *
     * @param userManager
     * @param groupManager
     * @param id
     * @param name
     */
    public YarepUser(UserManager userManager, GroupManager groupManager, String id, String name) {
        super(userManager, groupManager, id, name);
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    protected void configure(Configuration config) throws ConfigurationException, AccessManagementException {
        // Compulsory fields
        setID(config.getAttribute(ID));
        //log.debug("Read user profile: " + getID());

        setName(config.getChild(NAME, false).getValue(null));
        
        // Optional fields
        if(config.getChild(EMAIL, false) != null){
            setEmail(config.getChild(EMAIL, false).getValue(null));
        }
        
        if(config.getChild(PASSWORD, false) != null){
            // Do not use setter here because it does other things
            this.encryptedPassword = config.getChild(PASSWORD, false).getValue(null);
        }
        
        if(config.getChild(LANGUAGE, false) != null) {
            setLanguage(config.getChild(LANGUAGE, false).getValue(null));
        }
        
        if(config.getChild(SALT,false) != null) {
            this.salt = config.getChild(SALT, false).getValue(null);
        }
        
        if(config.getChild(DESCRIPTION, false) != null) {
            setDescription(config.getChild(DESCRIPTION, false).getValue(null));
        }
        
        if(config.getChild(EXPIRE, false) != null){
            SimpleDateFormat sdf1 = new SimpleDateFormat(DATE_TIME_FORMAT);
            SimpleDateFormat sdf2 = new SimpleDateFormat(DATE_FORMAT);
                String dateAsString = config.getChild(EXPIRE, false).getValue(null);
                Date expire = null;
                
                if(null != dateAsString){
                    try {
                        expire = sdf2.parse(dateAsString);
                    } catch (ParseException e) {
                        try {
                            expire = sdf1.parse(dateAsString);
                        } catch (ParseException e1) {
                            log.error(e.getMessage() + " (The user will be made expired)");
                        }finally{
                            if(expire == null){
                                GregorianCalendar cal = new GregorianCalendar();
                                cal.add(Calendar.YEAR, -10);
                                expire = cal.getTime();
                            }else{
                                // parsed correctly
                            }
                        }
                    }finally{
                        this.setExpirationDate(expire);
                    }
                }
        }

        Configuration groupsNode = config.getChild(GROUPS_TAG_NAME, false);
        if (groupsNode != null) {
            Configuration[] groupNodes = groupsNode.getChildren(GROUP_TAG_NAME);
            if (groupNodes != null && groupNodes.length > 0) {
                _groupIDs = new ArrayList();
                for (int i = 0; i < groupNodes.length; i++) {
                    _groupIDs.add(groupNodes[i].getAttribute(GROUP_ID_ATTR_NAME));
                }
            } else {
                log.warn("DEBUG: User '" + getID() + "' does not seem to belong to any groups.");
            }
        } else { // INFO: For backwards compatibility reason the group IDs are retrieved from the groups themselves and saved as bi-directional links
            log.warn("User '" + getID() + "' does seem to be an instance of a previous version without '" + GROUPS_TAG_NAME + "' tag and hence will be migrated automatically.");
            //if (fixGroupIndex) {
            if (true) {
                log.warn("Fix group index ...");
                String[] gids = getGroupIDs(false);
                _groupIDs = new ArrayList();
                for (int i = 0; i < gids.length; i++) {
                    _groupIDs.add(gids[i]);
                }
                save();
            }
        }
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#createConfiguration()
     */
    protected Configuration createConfiguration() throws AccessManagementException {
        DefaultConfiguration config = new DefaultConfiguration(USER);
        config.setAttribute(ID, getID());
        
        DefaultConfiguration nameNode = new DefaultConfiguration(NAME);
        nameNode.setValue(getName());
        config.addChild(nameNode);
        
        if(getEmail() != null){
            DefaultConfiguration emailNode = new DefaultConfiguration(EMAIL);
            emailNode.setValue(getEmail());
            config.addChild(emailNode);
        }
        
        if(getLanguage() != null){
            DefaultConfiguration languageNode = new DefaultConfiguration(LANGUAGE);
            languageNode.setValue(getLanguage());
            config.addChild(languageNode);
        }
        
        if(getPassword() != null){
            DefaultConfiguration passwordNode = new DefaultConfiguration(PASSWORD);
            passwordNode.setValue(getPassword());
            config.addChild(passwordNode);
        }
        
        if(getDescription() != null){
            DefaultConfiguration descriptionNode = new DefaultConfiguration(DESCRIPTION);
            descriptionNode.setValue(getDescription());
            config.addChild(descriptionNode);
        }
        
        if(getExpirationDate() != null){
            DefaultConfiguration expireNode = new DefaultConfiguration(EXPIRE);
            expireNode.setValue(new SimpleDateFormat(DATE_TIME_FORMAT).format(getExpirationDate()));
            config.addChild(expireNode);
        }
        
        if(getSalt() != null) {
            DefaultConfiguration saltNode = new DefaultConfiguration(SALT);
            saltNode.setValue(getSalt());
            config.addChild(saltNode);
        }

        if (_groupIDs != null && _groupIDs.size() > 0) {
            DefaultConfiguration groupsNode = new DefaultConfiguration(GROUPS_TAG_NAME);
            config.addChild(groupsNode);
            for (int i = 0; i < _groupIDs.size(); i++) {
                DefaultConfiguration groupNode = new DefaultConfiguration(GROUP_TAG_NAME);
                groupNode.setAttribute(GROUP_ID_ATTR_NAME, (String) _groupIDs.get(i));
                groupsNode.addChild(groupNode);
            }
        } else {
            DefaultConfiguration groupsNode = new DefaultConfiguration(GROUPS_TAG_NAME);
            config.addChild(groupsNode);
            _groupIDs = new ArrayList();
        }

        return config;
    }

    /**
     * @see org.wyona.security.core.api.User#authenticate(java.lang.String)
     */
    public boolean authenticate(String plainTextPassword) throws ExpiredIdentityException, AccessManagementException {
        if(isExpired()){
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
            throw new ExpiredIdentityException("Identity expired on "+sdf.format(getExpirationDate()));
        }
        
        if(getSalt() == null) {
            return getPassword().equals(Password.getMD5(plainTextPassword));
        } else {
            return getPassword().equals(Password.getMD5(plainTextPassword, getSalt()));
        }
    }

    /**
     * @deprecated Use org.wyona.security.impl.util.UserUtil#isExpired(User) instead
     */
    protected boolean isExpired() {
        return org.wyona.security.impl.util.UserUtil.isExpired(this);
    }
    
    public String getDescription() {
        return description;
    }

    /**
     * @see org.wyona.security.core.api.User#getEmail()
     */
    public String getEmail() throws AccessManagementException {
        return this.email;
    }

    /**
     * @see org.wyona.security.core.api.User#getLanguage()
     */
    public String getLanguage() throws AccessManagementException {
        return this.language;
    }

    /**
     * @see org.wyona.security.core.api.User#getSalt()
     */
    public String getSalt() throws AccessManagementException {
        return this.salt;
    }

    /**
     * @see org.wyona.security.core.api.User#getGroups()
     */
    public Group[] getGroups() throws AccessManagementException {
        if(log.isDebugEnabled()) log.debug("Get groups for user: " + getID() + ", " + getName());
        return getGroups(this);
    }

    /**
     * Get groups to which a particular item (user or group) belongs to or rather is listed in explicitely
     * @param item User or group
     */
    private Group[] getGroups(Item item) throws AccessManagementException {
        if(log.isDebugEnabled()) log.debug("Get groups for item: " + item.getID());

        ArrayList groups = new ArrayList();
        if (getGroupManager() != null) {
            if (item instanceof User) {
                String[] groupIDs = ((User) item).getGroupIDs(false);
                if (groupIDs != null) {
                    for (int i = 0; i < groupIDs.length; i++) {
                        groups.add(getGroupManager().getGroup(groupIDs[i]));
                    }
                } else {
                    log.warn("User '" + item.getID() + "' does not seem to belong to any groups.");
                }
            } else if (item instanceof Group)  {
                log.warn("DEPRECATED ...");
                Group[] allGroups = getGroupManager().getGroups();
                for (int i = 0; i < allGroups.length; i++) {
                    if (allGroups[i].isMember(item)) {
                        groups.add(allGroups[i]);
                    }
                }
            }
            return (Group[]) groups.toArray(new Group[groups.size()]);
        } else {
            log.error("Group manager is null!");
            return null;
        }
    }

    /**
     * @see org.wyona.security.core.api.User#getGroups(boolean)
     */
    public Group[] getGroups(boolean parents) throws AccessManagementException {
        if (parents) {
            log.info("Resolve parent groups for user '" + getID() + "' ...");
            Group[] groups = getGroups();
            Vector allGroups = new Vector();
            for (int i = 0; i < groups.length; i++) {
                allGroups.add(groups[i]);
            }
            for (int i = 0; i < groups.length; i++) {
                try {
                    getParentGroups(groups[i], allGroups);
                } catch (Exception e) {
                    log.error(e, e);
                    throw new AccessManagementException(e);
                }
            }
            return (Group[])allGroups.toArray(new Group[allGroups.size()]);
        } else {
            return getGroups();
        }
    }

    /**
     * @see org.wyona.security.core.api.User#getGroupIDs(boolean)
     */
    public String[] getGroupIDs(boolean parents) throws AccessManagementException {
        YarepGroupManager ygm = (YarepGroupManager) getGroupManager();
        if (ygm != null) {
            ArrayList<String> groupIDs = new ArrayList<String>();

            if (_groupIDs != null) {
                for (int i = 0; i < _groupIDs.size(); i++) {
                    groupIDs.add((String) _groupIDs.get(i));
                }
            } else {
                log.warn("Use deprecated implementation ...");
                Node[] groupNodes = ygm.getAllGroupNodes();
                for (int i = 0; i < groupNodes.length; i++) {
                    if (YarepGroup.isUserMember(groupNodes[i], getID())) {
                        try {
                            String groupID = YarepGroup.getGroupID(groupNodes[i]);
                            log.debug("User '" + getID() + "' is user member of group: " + groupID);
                            groupIDs.add(groupID);
                        } catch(Exception e) {
                            log.error(e, e);
                        }
                    }
                }
            }

            if (parents) {
                // TOOD: Replace this implementation
                log.info("Resolve parent groups for user '" + getID() + "' ...");
                ArrayList<String> groupIDsInclParents = new ArrayList<String>();
                for (int i = 0; i < groupIDs.size(); i++) {
                    groupIDsInclParents.add((String) groupIDs.get(i));
                }
                for (int i = 0; i < groupIDs.size(); i++) {
                    try {
                        getParentGroupIDs((String) groupIDs.get(i), groupIDsInclParents);
                    } catch(Exception e) {
                        log.error(e, e);
                    }
                }
                return (String[]) groupIDsInclParents.toArray(new String[groupIDsInclParents.size()]);
            } else {
                return (String[]) groupIDs.toArray(new String[groupIDs.size()]);
            }
        } else {
            log.error("Group manager is null!");
            return null;
        }
    }

    /**
     * Get parent groups of a particular group
     * @param group Particular group for which parent groups shall be found
     * @param groups Groups which have already been found
     */
    private void getParentGroups(Group group, Vector groups) throws Exception {
        Group[] parentGroups = getGroups(group);
        if (parentGroups != null && parentGroups.length > 0) {
            if (log.isDebugEnabled()) log.debug("Parent groups found for '" + group.getID() + "'");
            for (int i = 0; i < parentGroups.length; i++) {
                if (log.isDebugEnabled()) log.debug("Check if parent group '" + parentGroups[i].getID() + "' is already contained ...");
                boolean alreadyContained = false;
                for (int k = 0; k < groups.size(); k++) {
                    if (parentGroups[i].getID().equals(((Group)groups.elementAt(k)).getID())) {
                        log.warn("Loop detected for group '" + group.getID() + "' and parent group '" + parentGroups[i].getID() + "'!");
                        alreadyContained = true;
                        break;
                    }
                }
                if (!alreadyContained) {
                    if (log.isDebugEnabled()) log.debug("Add parent group '" + parentGroups[i].getID() + "'!");
                    groups.add(parentGroups[i]);
                    getParentGroups(parentGroups[i], groups);
                }
            }
        } else {
            if (log.isDebugEnabled()) log.debug("Group '" + group.getID() + "' does not seem to have parent groups.");
        }
    }

    /**
     * @see org.wyona.security.core.api.User#setEmail(java.lang.String)
     */
    public void setEmail(String email) throws AccessManagementException {
        this.email = email;
    }

    /**
     * @see org.wyona.security.core.api.User#setLanguage(java.lang.String)
     */
    public void setLanguage(String language) throws AccessManagementException {
        this.language = language;
    }

    /**
     * @see org.wyona.security.core.api.User#setPassword(java.lang.String)
     */
    public void setPassword(String plainTextPassword) throws AccessManagementException {
        setSalt();
        this.encryptedPassword = Password.getMD5(plainTextPassword, this.salt);
    }

    /**
     * @see org.wyona.security.core.api.User#setSalt()
     */
    public void setSalt() throws AccessManagementException {
        this.salt = Password.getSalt();

    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the password hash
     * @return encrypted password
     * @throws AccessManagementException
     */
    protected String getPassword() throws AccessManagementException {
        return this.encryptedPassword;
    }

    /**
     *
     */
    public Date getExpirationDate() {
        return expire;
    }

    /**
     *
     */
    public void setExpirationDate(Date date) {
        this.expire = date;
    }

    /**
     *
     */
    public UserHistory getHistory() {
        log.error("TODO: Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public void setHistory(UserHistory history) {
        log.error("TODO: Not implemented yet!");
    }
    
    /**
     * Two users are equal if they have the same id.
     */
    public boolean equals(Object obj) {
        if (obj instanceof User) {
            String id1;
            try {
                id1 = getID();
                String id2 = ((User)obj).getID();
                return id1.equals(id2);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Get parent group IDs of a particular group
     * @param groupID ID of particular group
     * @param groupIDsInclParents Group IDs which have already been found
     */
    public void getParentGroupIDs(String groupID, ArrayList<String> groupIDsInclParents) throws Exception {
        log.debug("Get parent group IDs for particular group: " + groupID);

        YarepGroupManager ygm = (YarepGroupManager) getGroupManager();
        if (ygm != null) {
            Node[] groupNodes = ygm.getAllGroupNodes();
            for (int i = 0; i < groupNodes.length; i++) {
                if (YarepGroup.isGroupMember(groupNodes[i], groupID)) {
                    try {
                        String parentGroupID = YarepGroup.getGroupID(groupNodes[i]);
                        log.debug("Group '" + groupID + "' is group member of parent group: " + parentGroupID);
                        boolean alreadyContained = false;
                        for (int k = 0; k < groupIDsInclParents.size(); k++) {
                            if (groupIDsInclParents.get(k).equals(parentGroupID)) {
                                log.warn("Loop detected for group '" + groupID + "' and parent group '" + parentGroupID + "'!");
                                alreadyContained = true;
                                break;
                            }
                        }
                        if (!alreadyContained) {
                            if (log.isDebugEnabled()) log.debug("Add parent group '" + parentGroupID + "'!");
                            groupIDsInclParents.add(parentGroupID);
                            getParentGroupIDs(parentGroupID, groupIDsInclParents);
                        }
                    } catch(Exception e) {
                        log.error(e, e);
                    }
                }
            }
        } else {
            log.error("Group manager is null!");
        }
    }

    /**
     * Add group (creating a bi-directional link)
     * @param id Group ID
     */
    void addGroup(String id) throws AccessManagementException {
        log.warn("DEBUG: Add user '" + getID() + "' to group: " + id);
        if (_groupIDs == null) {
            throw new AccessManagementException("User '" + getID() + "' has groups not initialized yet!");
        }
        if (_groupIDs.indexOf(id) < 0) {
            _groupIDs.add(id);
        } else {
            throw new AccessManagementException("User '" + getID() + "' already belongs to group '" + id + "'!");
        }
        save();
    }

    /**
     * Remove group (Remove bi-directional link)
     * @param id Group ID
     */
    void removeGroup(String id) throws AccessManagementException {
        log.warn("DEBUG: Remove user '" + getID() + "' from group '" + id + "'.");
        if (_groupIDs != null) {
            if (_groupIDs.indexOf(id) >= 0) {
                _groupIDs.remove(_groupIDs.indexOf(id));
            } else {
                throw new AccessManagementException("User '" + getID() + "' does not belong to group '" + id + "'!");
            }
        } else {
            throw new AccessManagementException("User '" + getID() + "' has no groups!");
        }
        save();
    }
}
