package com.example.notificationservice;
import com.example.notificationservice.controller.NotificationController;
import com.example.notificationservice.service.NotificationService;
import com.example.notificationservice.security.NotificationIdentityResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class NotificationServiceApplicationTests {
 @Test void foreignUserCannotListOrMarkNotifications(){
  var service=mock(NotificationService.class);var identity=mock(NotificationIdentityResolver.class);
  doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(identity).requireOwner(8L,"Bearer test");
  var controller=new NotificationController(service,identity);
  assertThrows(ResponseStatusException.class,()->controller.getUserNotifications(8L,"Bearer test"));
  assertThrows(ResponseStatusException.class,()->controller.markAsRead("foreign",8L,"Bearer test"));
  verifyNoInteractions(service);
 }
 @Test void missingBearerIsUnauthorized(){var resolver=new NotificationIdentityResolver("http://127.0.0.1:1");var e=assertThrows(ResponseStatusException.class,()->resolver.resolve(null));assertEquals(401,e.getStatusCode().value());}
}
