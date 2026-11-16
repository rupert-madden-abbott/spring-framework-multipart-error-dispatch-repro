# sendError on a multipart request breaks the error page, as DispatcherServlet parses the body again on the error dispatch

When a filter rejects a `multipart/form-data` request with `response.sendError(...)`, the container dispatches to the error page, and `DispatcherServlet.checkMultipart` parses the multipart body again during that error dispatch. It ignores a parse failure there only when `jakarta.servlet.error.exception` is set, which a plain `sendError` doesn't set, so the failure escapes and replaces the intended error response.

On Jetty, which on `sendError` consumes what has arrived of the unread body and fails the rest, parsing always fails (`EOFException: unexpected EOF in PREAMBLE` for a small body, `Unconsumed` for a large one): a small request gets the status with an empty body, and a large one gets Jetty's own HTML error page instead of the application's. On Tomcat, which keeps the body, the whole upload of an already rejected request is parsed, and one over `spring.servlet.multipart.max-file-size` gets 413 instead of the status `sendError` set.

**Run:** JDK 17 or later, then:

- `mvn test` runs on Jetty.
- `mvn test -Ptomcat` runs on Tomcat.

A servlet filter calls `sendError(401)` for every request to `/upload`. Each test POSTs a multipart request there and expects Spring Boot's error page:

- `sendErrorOnSmallMultipartRequestRendersErrorPage`: a 7-byte file.
- `sendErrorOnLargeMultipartRequestRendersErrorPage`: a 10 MB file, over the default 1 MB `max-file-size`.

**Expected:**

- Both tests: 401 with Spring Boot's JSON error body.

**Actual:** On Jetty both tests fail; on Tomcat the large one fails.

Jetty:

```
[ERROR] Failures: 
[ERROR]   MultipartErrorDispatchTest.sendErrorOnLargeMultipartRequestRendersErrorPage:31->assertErrorPage:37 
Expecting actual:
  "<html>
<head>
<meta http-equiv="Content-Type" content="text/html;charset=ISO-8859-1"/>
<title>Error 401 Unauthorized</title>
</head>
<body>
<h2>HTTP ERROR 401 Unauthorized</h2>
<table>
<tr><th>URI:</th><td>http://localhost:36693/upload</td></tr>
<tr><th>STATUS:</th><td>401</td></tr>
<tr><th>MESSAGE:</th><td>Unauthorized</td></tr>
<tr><th>SERVLET:</th><td>dispatcherServlet</td></tr>
</table>

</body>
</html>
"
to contain:
  ""status":401" 
[ERROR]   MultipartErrorDispatchTest.sendErrorOnSmallMultipartRequestRendersErrorPage:26->assertErrorPage:37 
Expecting actual:
  ""
to contain:
  ""status":401" 
[ERROR] Tests run: 2, Failures: 2, Errors: 0, Skipped: 0
```

Tomcat:

```
[ERROR] Failures: 
[ERROR]   MultipartErrorDispatchTest.sendErrorOnLargeMultipartRequestRendersErrorPage:31->assertErrorPage:36 
expected: 401
 but was: 413
[ERROR] Tests run: 2, Failures: 1, Errors: 0, Skipped: 0
```

**Versions:** Spring Boot 4.1.2, Spring Framework 7.0.10, Jetty 12.1.14, Tomcat 11.0.27, Java 27. `DispatcherServlet.checkMultipart` is unchanged on Spring Framework `main`.
