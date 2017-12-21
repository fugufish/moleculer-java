package services.moleculer.eventbus;

import java.util.concurrent.ExecutorService;

import io.datatree.Tree;
import services.moleculer.ServiceBroker;

public class LocalListenerEndpoint extends ListenerEndpoint {

	// --- PROPERTIES ---

	/**
	 * Listener instance (it's a field / inner class in Service object)
	 */
	protected Listener listener;

	/**
	 * Invoke all local listeners via Thread pool (true) or directly (false)
	 */
	protected boolean asyncLocalInvocation;

	// --- COMPONENTS ---

	protected ExecutorService executor;

	// --- CONSTRUCTOR ---

	public LocalListenerEndpoint(Listener listener, boolean asyncLocalInvocation) {
		this.listener = listener;
		this.asyncLocalInvocation = asyncLocalInvocation;
	}

	// --- START CONTAINER ---

	/**
	 * Initializes Container instance.
	 * 
	 * @param broker
	 *            parent ServiceBroker
	 * @param config
	 *            optional configuration of the current component
	 */
	@Override
	public void start(ServiceBroker broker, Tree config) throws Exception {

		// Set base properties
		super.start(broker, config);

		// Process config
		asyncLocalInvocation = config.get("asyncLocalInvocation", asyncLocalInvocation);

		// Set components
		if (asyncLocalInvocation) {
			executor = broker.components().executor();
		}
	}

	// --- INVOKE LOCAL LISTENER ---

	@Override
	public void on(String name, Tree payload, Groups groups, boolean emit) throws Exception {

		// A.) Async invocation
		if (asyncLocalInvocation) {
			executor.execute(() -> {
				try {
					listener.on(payload);
				} catch (Exception cause) {
					logger.warn("Unable to invoke local listener!", cause);
				}
			});
			return;
		}

		// B.) Faster in-process (direct) invocation
		listener.on(payload);
	}

	@Override
	public boolean local() {
		return true;
	}

}