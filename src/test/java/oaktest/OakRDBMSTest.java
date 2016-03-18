package oaktest;

import java.io.File;
import java.io.IOException;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.CommitFailedException;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.api.ContentSession;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore;
import org.apache.jackrabbit.oak.plugins.document.DocumentStore;
import org.apache.jackrabbit.oak.plugins.document.rdb.RDBDocumentStore;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.spi.blob.BlobStore;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@Test
@ContextConfiguration(locations = { "classpath:config/spring/spring-test-config.xml" })
public class OakRDBMSTest extends AbstractTestNGSpringContextTests {
	@Autowired
	@Qualifier("oakPostgresDataSource")
	private DataSource ds;
	/**
	 * method create a new type folder and add a node; target configuration: filesystem and Postgres RDBMS
	 * we create a documentnodestore with a private method:getRDBDocumentNodeStore
	 * 
	 * @throws javax.security.auth.login.LoginException
	 * @throws NoSuchWorkspaceException
	 * @throws CommitFailedException
	 */
	@Test
	public void test() throws javax.security.auth.login.LoginException, NoSuchWorkspaceException, CommitFailedException {
		
		final DocumentMK.Builder builder = new DocumentMK.Builder();

		InitialContent ic = new InitialContent();
		DocumentNodeStore ns = getRDBDocumentNodeStore(builder);
		
		// we want to use jcr interface if is possibile; so our implementation start with jcr repository
		// through which we create default workspace with admin/admin.
		Oak oak = new Oak(ns).with(ic);
		Jcr jcr = new Jcr(oak);
		
		Repository repo = jcr.createRepository();
		
		Session session = null;
		try {
			session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
			
			createNode(session);
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (session != null) session.logout();
			ns.dispose();
		}
	}

	private void createNode(Session session) throws UnsupportedRepositoryOperationException, RepositoryException {
		NodeTypeTemplate nt = session.getWorkspace().getNodeTypeManager().createNodeTypeTemplate();
		nt.setName("FOLDERCONTESTO");
		nt.setDeclaredSuperTypeNames(new String[] {"nt:folder"});
		PropertyDefinitionTemplate pdt = session.getWorkspace().getNodeTypeManager().createPropertyDefinitionTemplate();
		pdt.setName("text");
		pdt.setFullTextSearchable(true);
		pdt.setRequiredType(PropertyType.STRING);
		nt.getPropertyDefinitionTemplates().add(pdt);
		session.getWorkspace().getNodeTypeManager().registerNodeType(nt, true);
		
		try {
			Node node = session.getRootNode().addNode("altro test rdbms", "FOLDERCONTESTO");
			node.setProperty("text", "munch");
		} catch (Exception e) {
			// TODO: handle exception
		}finally {
			session.save();
		}
		
		
	}
	
	
	/**
	 * method create a new type folder and add a node; target configuration: filesystem and Postgres RDBMS
	 * in this test we create a documentnodestore with via  RDBDocumentStore
	 * 
	 * @throws javax.security.auth.login.LoginException
	 * @throws NoSuchWorkspaceException
	 * @throws CommitFailedException
	 */
	@Test
	public void test1() throws javax.security.auth.login.LoginException, NoSuchWorkspaceException, CommitFailedException {
		
		final DocumentMK.Builder builder = new DocumentMK.Builder();
		builder.setBlobStore(createFileSystemBlobStore());
		
	    DocumentStore documentStore = new RDBDocumentStore(ds, builder);    

	    builder.setDocumentStore(documentStore);
	    DocumentNodeStore ns = new DocumentNodeStore(builder); 
		
		
		InitialContent ic = new InitialContent();
		
		// we want to use jcr interface if is possibile; so our implementation start with jcr repository
		// through which we create default workspace with admin/admin.
		Oak oak = new Oak(ns).with(ic);
		Jcr jcr = new Jcr(oak);
		
		Repository repo = jcr.createRepository();
		
		Session session = null;
		try {
			session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));
			
			createNode(session);
		} catch (LoginException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (session != null) session.logout();
			ns.dispose();
		}
	}

	

	private DocumentNodeStore getRDBDocumentNodeStore(DocumentMK.Builder builder) {
		DocumentNodeStore ns = null;
		if (builder == null) {
			ns = new DocumentMK.Builder().setRDBConnection(ds).getNodeStore();
		} else {
			ns = builder.setRDBConnection(ds).getNodeStore();
		}
		return ns;
	}

	private BlobStore createFileSystemBlobStore() {
		try {
			FileUtils.deleteDirectory(new File("/var/tmp/oak"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		FileBlobStore store = new FileBlobStore("/var/tmp/oak");
		return store;
	}

	 private boolean nodeExists(final Session session, final String nodePath) throws RepositoryException{
	        try {
	            session.getNode(nodePath);
	            return true;
	        }
	        catch (Exception e) {
	            return false;
	        }
	    }
}
