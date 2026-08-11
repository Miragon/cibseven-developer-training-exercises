package io.miragon.training.adapter.outbound.engine;

import io.miragon.training.application.port.outbound.WelcomeKitProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Drives the (remote) engine to start the OWNED {@code sendWelcomeKit} process (Direction 2: worker -> engine).
 *
 * <p>TODO Aufgabe 9: use the generated, typed client. Once you enabled the generator (see pom.xml) and
 * {@code EngineClientConfig} provides the {@code ProcessDefinitionApi} bean, inject it here and start the
 * process by key, passing the member name as a typed variable:
 *
 * <pre>
 *   // imports: org.cibseven.rest.client.api.ProcessDefinitionApi,
 *   //          org.cibseven.rest.client.model.StartProcessInstanceDto,
 *   //          org.cibseven.rest.client.model.VariableValueDto
 *   private final ProcessDefinitionApi processDefinitionApi;
 *
 *   public RemoteWelcomeKitProcessAdapter(ProcessDefinitionApi processDefinitionApi) {
 *       this.processDefinitionApi = processDefinitionApi;
 *   }
 *
 *   var request = new StartProcessInstanceDto()
 *       .variables(Map.of("name", new VariableValueDto().value(memberName).type("String")));
 *   processDefinitionApi.startProcessInstanceByKey(SendWelcomeKitProcessApi.PROCESS_ID.getValue(), request);
 * </pre>
 */
@Component
public class RemoteWelcomeKitProcessAdapter implements WelcomeKitProcess {

    private static final Logger log = LoggerFactory.getLogger(RemoteWelcomeKitProcessAdapter.class);

    @Override
    public void startWelcomeKit(String memberName) {
        throw new UnsupportedOperationException(
                "TODO Aufgabe 9: start the process via the generated client (ProcessDefinitionApi)");
    }
}
