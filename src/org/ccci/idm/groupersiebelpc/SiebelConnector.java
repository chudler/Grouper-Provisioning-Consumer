package org.ccci.idm.groupersiebelpc;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;

import edu.internet2.middleware.grouper.changeLog.provisioning.ConfigItem;
import edu.internet2.middleware.grouper.changeLog.provisioning.EventProvisioningConnector;

import org.ccci.framework.sblio.SiebelPersistence;
import org.ccci.framework.sblio.SiebelPersistenceImpl;
import org.ccci.idm.grouper.dao.GrouperDao;
import org.ccci.idm.grouper.dao.GrouperDaoImpl;
import org.ccci.idm.grouper.obj.GrouperGroup;
import org.ccci.idm.obj.SsoUser;
import org.ccci.idm.siebel.dao.SiebelContactDao;
import org.ccci.idm.siebel.dao.SiebelUserDao;
import org.ccci.idm.siebel.dao.jdb.SiebelContactDaoJdb;
import org.ccci.idm.siebel.dao.jdb.SiebelUserDaoJdb;
import org.ccci.idm.siebel.obj.SiebelContact;
import org.ccci.idm.siebel.obj.SiebelUser;

public class SiebelConnector implements EventProvisioningConnector
{
    private static final String SIEBEL_GROUP_PREFIX = "ccci:itroles:uscore:siebel_resp";
    private static final String SIEBEL_DEFAULT_PRIMARY_RESPONSIBILITY = "CCCI Base User";

    private GrouperDao dao;

    @ConfigItem
    private String username;
    @ConfigItem
    private String password;
    @ConfigItem
    private String url;

    public SiebelConnector()
    {
        super();
    }

    public boolean dispatchEvent(ChangeEvent event) throws Exception
    {
        if (dao == null)
        {
            dao = new GrouperDaoImpl(null);
        }

        if (event.getGroupName().startsWith(SIEBEL_GROUP_PREFIX))
        {
            SiebelPersistence siebelPersistence = new SiebelPersistenceImpl(username, password, url);
            SiebelContactDao siebelContactDao = new SiebelContactDaoJdb(siebelPersistence);
            SiebelUserDao siebelUserDao = new SiebelUserDaoJdb(siebelPersistence);

            SsoUser user = dao.loadSsoUser(event.getSubjectId());

            if (user == null)
                throw new Exception("Could not load sso user " + event.getSubjectId());

            SiebelContact siebelContact = siebelContactDao.getContactByGuid(user.getSsoGuid());
            SiebelUser siebelUser = siebelUserDao.getUserByAccountId(siebelContact.getAccountId());

            GrouperGroup group = dao.loadGroup(event.getGroupName());

            if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_ADD.name()))
            {
                siebelUserDao.addResponsibility(siebelUser.getLoginName(), group.getDisplayName(), 
                                                group.getDisplayName().equals(SIEBEL_DEFAULT_PRIMARY_RESPONSIBILITY) ? true : false);
                return true;
            }
            else if (event.getEventType().equals(ChangeEvent.ChangeEventType.MEMBERSHIP_DELETE.name()))
            {
                siebelUserDao.deleteResponsibility(siebelUser.getLoginName(), group.getDisplayName());
                return true;
            }
        }

        return false;
    }

    public void close()
    {
        if (dao != null)
            dao.close();
        dao = null;
    }

    public void flush()
    {
    }

    public void init(String consumerName)
    {
    }
}
