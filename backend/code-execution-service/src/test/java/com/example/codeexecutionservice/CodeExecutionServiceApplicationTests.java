package com.example.codeexecutionservice;
import com.example.codeexecutionservice.controller.CodeExecutionController;
import com.example.codeexecutionservice.dto.CodeExecutionRequest;
import com.example.codeexecutionservice.service.CodeExecutionService;
import com.example.codeexecutionservice.security.ExecutionIdentityResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class CodeExecutionServiceApplicationTests {
 @Test void unauthorizedRequestNeverReachesDocker(){
  var executor=mock(CodeExecutionService.class);var identity=mock(ExecutionIdentityResolver.class);
  when(identity.resolve(null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
  var controller=new CodeExecutionController(executor,identity);
  assertThrows(ResponseStatusException.class,()->controller.execute(null,new CodeExecutionRequest("python","print(1)",null)));
  verifyNoInteractions(executor);
 }
 @Test void missingBearerFailsBeforeCoreRequest(){var resolver=new ExecutionIdentityResolver("http://127.0.0.1:1");var e=assertThrows(ResponseStatusException.class,()->resolver.resolve(null));assertEquals(401,e.getStatusCode().value());}
}
