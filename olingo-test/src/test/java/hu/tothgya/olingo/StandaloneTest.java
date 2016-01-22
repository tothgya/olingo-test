package hu.tothgya.olingo;

import java.io.StringWriter;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class StandaloneTest {

	private static final Logger logger = Logger.getLogger(StandaloneTest.class);
	
	private static Server server;
	
	@BeforeClass
	public static void setup() throws Exception {
		server = new Server(8180);
		
		final ServletHandler servletHandler = new ServletHandler();
		final ServletHolder servletHolder = new ServletHolder(org.apache.cxf.jaxrs.servlet.CXFNonSpringJaxrsServlet.class);
		servletHolder.setInitParameter(
				"javax.ws.rs.Application",
				"org.apache.olingo.odata2.core.rest.app.ODataApplication");
		servletHolder.setInitParameter(
				"org.apache.olingo.odata2.service.factory",
				"hu.tothgya.olingo.TestServiceFactory");
		servletHolder.setInitOrder(1);
		
		servletHandler.addServletWithMapping(servletHolder, "/svc/*");
		
        server.setHandler(servletHandler);
        
        server.start();
	}

	@AfterClass
	public static void tearDown() throws Exception{
		server.stop();
		server.destroy();
	}
	
	@Test
	public void test() throws Exception {
		final EntityManagerFactory emf = Persistence.createEntityManagerFactory("test");
		
		TestEntity t1 = new TestEntity();
		t1.setData("t1");
		TestEntity t2 = new TestEntity();
		t2.setData("t2");
		t2.setParent(t1);
		TestEntity t3 = new TestEntity();
		t3.setData("t3");
		t3.setParent(t2);
		
		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();
		em.persist(t1);
		em.persist(t2);
		em.persist(t3);
		em.getTransaction().commit();
		em.close();
		
		HttpClient client = HttpClientBuilder.create().build();
		
//		final HttpGet request = new HttpGet("http://localhost:8180/svc/$metadata");
		final HttpGet request = new HttpGet("http://localhost:8180/svc/TestEntitys(3)?$expand=parent,parent/parent");
		HttpResponse result = client.execute(request);
		

		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		
		StreamResult streamResult = new StreamResult(new StringWriter());
		StreamSource source = new StreamSource(result.getEntity().getContent());
		transformer.transform(source, streamResult);
		String xmlString = streamResult.getWriter().toString();
		logger.info("\r\n" + xmlString);
		
//		BufferedReader in = new BufferedReader(new InputStreamReader(result.getEntity().getContent()));
//	
//		for(String line = in.readLine(); line != null; line = in.readLine()) {
//			logger.info(line);
//		}
		
		logger.info(result);
		
	}
}
