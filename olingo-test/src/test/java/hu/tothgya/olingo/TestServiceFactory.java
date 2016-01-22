package hu.tothgya.olingo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;

import org.apache.log4j.Logger;
import org.apache.olingo.odata2.api.ODataCallback;
import org.apache.olingo.odata2.api.ODataDebugCallback;
import org.apache.olingo.odata2.api.ODataService;
import org.apache.olingo.odata2.api.ODataServiceFactory;
import org.apache.olingo.odata2.api.edm.provider.EdmProvider;
import org.apache.olingo.odata2.api.exception.ODataException;
import org.apache.olingo.odata2.api.processor.ODataContext;
import org.apache.olingo.odata2.api.processor.ODataErrorCallback;
import org.apache.olingo.odata2.api.processor.ODataSingleProcessor;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext;
import org.apache.olingo.odata2.jpa.processor.api.ODataJPATransaction;
import org.apache.olingo.odata2.jpa.processor.api.OnJPAWriteContent;
import org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPAErrorCallback;
import org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPARuntimeException;
import org.apache.olingo.odata2.jpa.processor.api.factory.ODataJPAAccessFactory;
import org.apache.olingo.odata2.jpa.processor.api.factory.ODataJPAFactory;
import org.apache.olingo.odata2.jpa.processor.core.ODataJPAProcessorDefault;

public class TestServiceFactory extends ODataServiceFactory {

	public interface Factory {
		<T> T create();
		boolean is();
	}
	
	private static final class ODataJPATransactionFactory implements Factory {
		private final Class<? extends ODataCallback> callbackInterface;
		private final ODataJPATransaction oDataJPATransaction;

		public ODataJPATransactionFactory(
				final Class<? extends ODataCallback> callbackInterface,
				final ODataJPATransaction oDataJPATransaction) {
			this.callbackInterface = callbackInterface;
			this.oDataJPATransaction = oDataJPATransaction;
		}

		@Override
		public boolean is() {
			return oDataJPATransaction != null && callbackInterface.isAssignableFrom(ODataJPATransaction.class);
		}

		@Override
		public <T> T create() {
			return (T) oDataJPATransaction;
		}
	}

	private static final class OnJPAWriteContentFactory implements Factory {
		private final Class<? extends ODataCallback> callbackInterface;
		private final OnJPAWriteContent onJPAWriteContent;

		public OnJPAWriteContentFactory(Class<? extends ODataCallback> callbackInterface, OnJPAWriteContent onJPAWriteContent) {
			this.callbackInterface = callbackInterface;
			this.onJPAWriteContent = onJPAWriteContent;
		}

		@Override
		public boolean is() {
			return onJPAWriteContent != null && callbackInterface.isAssignableFrom(OnJPAWriteContent.class);
		}

		@Override
		public <T> T create() {
			return (T) onJPAWriteContent;
		}
	}

	private static final class ODataJPAErrorCallbackFactory implements Factory {
		private final Class<? extends ODataCallback> callbackInterface;
		private final boolean setDetailErrors;

		public ODataJPAErrorCallbackFactory(Class<? extends ODataCallback> callbackInterface, boolean setDetailErrors) {
			this.callbackInterface = callbackInterface;
			this.setDetailErrors = setDetailErrors;
		}

		@Override
		public boolean is() {
			return setDetailErrors && callbackInterface.isAssignableFrom(ODataErrorCallback.class);
		}

		@Override
		public <T> T create() {
			return (T) new ODataJPAErrorCallback();
		}
	}

	private static final class ScenarioDebugCallbackFactory implements Factory {
		private final Class<? extends ODataCallback> callbackInterface;

		public ScenarioDebugCallbackFactory(Class<? extends ODataCallback> callbackInterface) {
			this.callbackInterface = callbackInterface;
		}

		@Override
		public boolean is() {
			return callbackInterface.isAssignableFrom(ODataDebugCallback.class);
		}

		@Override
		public <T> T create() {
			return (T) new ScenarioDebugCallback();
		}
	}

	private static final class ScenarioErrorCallbackFactory implements Factory {
		private final Class<? extends ODataCallback> callbackInterface;

		public ScenarioErrorCallbackFactory(Class<? extends ODataCallback> callbackInterface) {
			this.callbackInterface = callbackInterface;
		}

		@Override
		public boolean is() {
			return callbackInterface.isAssignableFrom(ScenarioErrorCallback.class);
		}

		@Override
		public <T> T create() {
			return (T) new ScenarioErrorCallback();
		}
	}

	private static final class ScenarioDebugCallback implements ODataDebugCallback {
		@Override
		public boolean isDebugEnabled() {
			return true;
		}
	}

	private static final class ScenarioErrorCallback implements ODataDebugCallback {
		@Override
		public boolean isDebugEnabled() {
			return true;
		}
	}

	private static final Logger logger = Logger.getLogger(TestServiceFactory.class);

	private static final String PUNIT_NAME = "test";
	private ODataContext oDataContext;
	private ODataJPAContext oDataJPAContext;
	private ODataJPATransaction oDataJPATransaction = null;
	private OnJPAWriteContent onJPAWriteContent = null;

	private boolean setDetailErrors = false;

	/**
	 * Creates an OData Service based on the values set in
	 * {@link org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext} and
	 * {@link org.apache.olingo.odata2.api.processor.ODataContext}.
	 */
	@Override
	public final ODataService createService(final ODataContext ctx) throws ODataException {

		oDataContext = ctx;

		// Initialize OData JPA Context
		oDataJPAContext = initializeODataJPAContext();

		validatePreConditions();

		ODataJPAFactory factory = ODataJPAFactory.createFactory();
		ODataJPAAccessFactory accessFactory = factory.getODataJPAAccessFactory();

		// OData JPA Processor
		if (oDataJPAContext.getODataContext() == null) {
			oDataJPAContext.setODataContext(ctx);
		}

		ODataSingleProcessor odataJPAProcessor = new ODataJPAProcessorDefault(oDataJPAContext);

		// OData Entity Data Model Provider based on JPA
		EdmProvider edmProvider = accessFactory.createJPAEdmProvider(oDataJPAContext);

		return createODataSingleProcessorService(edmProvider, odataJPAProcessor);
	}
	
	@Override
	public <T extends ODataCallback> T getCallback(final Class<T> callbackInterface) {

		final List<Factory> factories = new ArrayList<TestServiceFactory.Factory>();

		factories.add(new ScenarioErrorCallbackFactory(callbackInterface));
		
		factories.add(new ScenarioDebugCallbackFactory(callbackInterface));

		factories.add(new ODataJPAErrorCallbackFactory(callbackInterface, setDetailErrors));

		factories.add(new OnJPAWriteContentFactory(callbackInterface, onJPAWriteContent));

		factories.add(new ODataJPATransactionFactory(callbackInterface, oDataJPATransaction));
		
		
		for(final Factory factory: factories) {
			if(factory.is()) {
				return factory.create();
			}
		}
		
		return null;
	}

	/**
	 * @return an instance of type {@link ODataJPAContext}
	 * @throws ODataJPARuntimeException
	 */
	public final ODataJPAContext getODataJPAContext() throws ODataJPARuntimeException {
		if (oDataJPAContext == null) {
			oDataJPAContext = ODataJPAFactory.createFactory().getODataJPAAccessFactory().createODataJPAContext();
		}
		if (oDataContext != null) {
			oDataJPAContext.setODataContext(oDataContext);
		}
		return oDataJPAContext;

	}

	/**
	 * Implement this method and initialize OData JPA Context. It is mandatory
	 * to set an instance of type {@link javax.persistence.EntityManagerFactory}
	 * into the context. An exception of type
	 * {@link org.apache.olingo.odata2.jpa.processor.api.exception.ODataJPARuntimeException}
	 * is thrown if EntityManagerFactory is not initialized. <br>
	 * <br>
	 * <b>Sample Code:</b> <code>
	 * <p>public class JPAReferenceServiceFactory extends ODataJPAServiceFactory{</p>
	 * 
	 * <blockquote>private static final String PUNIT_NAME = "punit";
	 * <br>
	 * public ODataJPAContext initializeODataJPAContext() {
	 * <blockquote>ODataJPAContext oDataJPAContext = this.getODataJPAContext();
	 * <br>
	 * EntityManagerFactory emf = Persistence.createEntityManagerFactory(PUNIT_NAME);
	 * <br>
	 * oDataJPAContext.setEntityManagerFactory(emf);
	 * oDataJPAContext.setPersistenceUnitName(PUNIT_NAME);
	 * <br> return oDataJPAContext;</blockquote>
	 * }</blockquote>
	 * } </code>
	 * <br>	
	 * <br>
	 * @return an instance of type
	 *         {@link org.apache.olingo.odata2.jpa.processor.api.ODataJPAContext}
	 * @throws ODataJPARuntimeException
	 */
	public ODataJPAContext initializeODataJPAContext() {
		try {
			final ODataJPAContext context = getODataJPAContext();

			final EntityManagerFactory emf = Persistence.createEntityManagerFactory(PUNIT_NAME);

			for (final EntityType<?> e : emf.getMetamodel().getEntities()) {
				logger.info(e.getName());
			}

			context.setEntityManagerFactory(emf);
			context.setPersistenceUnitName(PUNIT_NAME);

			context.setDefaultNaming(false);

			return context;
		} catch (ODataJPARuntimeException e) {
			logger.error("Failed to create context", e);

			return null;
		}
	}

	/**
	 * The method sets the context whether a detail error message should be
	 * thrown or a less detail error message should be thrown by the library.
	 * 
	 * @param setDetailErrors
	 *            takes
	 *            <ul>
	 *            <li>true - to indicate that library should throw a detailed
	 *            error message</li>
	 *            <li>false - to indicate that library should not throw a
	 *            detailed error message</li>
	 *            </ul>
	 * 
	 */
	protected void setDetailErrors(final boolean setDetailErrors) {
		this.setDetailErrors = setDetailErrors;
	}

	/**
	 * The methods sets the context with a callback implementation for JPA
	 * transaction specific content. For details refer to
	 * {@link ODataJPATransaction}
	 * 
	 * @param oDataJPATransaction
	 *            is an instance of type
	 *            {@link org.apache.olingo.odata2.jpa.processor.api.ODataJPATransaction}
	 */
	protected void setODataJPATransaction(final ODataJPATransaction oDataJPATransaction) {
		this.oDataJPATransaction = oDataJPATransaction;
	}

	/**
	 * The methods sets the context with a callback implementation for JPA
	 * provider specific content. For details refer to
	 * {@link org.apache.olingo.odata2.jpa.processor.api.OnJPAWriteContent}
	 * 
	 * @param onJPAWriteContent
	 *            is an instance of type
	 *            {@link org.apache.olingo.odata2.jpa.processor.api.OnJPAWriteContent}
	 */
	protected void setOnWriteJPAContent(final OnJPAWriteContent onJPAWriteContent) {
		this.onJPAWriteContent = onJPAWriteContent;
	}

	private void validatePreConditions() throws ODataJPARuntimeException {

		if (oDataJPAContext.getEntityManagerFactory() == null) {
			throw ODataJPARuntimeException.throwException(ODataJPARuntimeException.ENTITY_MANAGER_NOT_INITIALIZED,
					null);
		}

	}

}