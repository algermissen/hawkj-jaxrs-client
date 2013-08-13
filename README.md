hawkj-jaxrs-client
==================

JAX-RS 2 client side filter for making Hawk authenticated requests

Status
======

API is relatively stable, but might change until reaching 1.0.


Example Use
===========

Below is example code, showing how to use hawkj-jaxrs-client together with
JAX-RS 2 client API.


    /*
     * These are credentials the client developer has obtained from the
     * resource owner. Maybe during the client registration process.
     */
    String clientId = "client0005";
    String clientPwd = "6h8Hgg$#jak";
    Algorithm algorithm = Algorithm.SHA_256;

    
    /*
     * Credentials and HawkClientProvider need to be given to filter
     * when constructed. We use a simple, illustrative version here.
     * These could well be backed by a database in a real application.
     */
    final SimpleHawkCredentials credentials = new SimpleHawkCredentials(
            clientId, clientPwd, algorithm);
    SimpleHawkClientProvider provider = new SimpleHawkClientProvider(credentials);

    HawkClientFilter hawkFilter = new HawkClientFilter(provider);
    hawkFilter.setIsHashRequestPayload(false);
    hawkFilter.setIsValidateResponsePayload(false);
    
    /*
     * Prepare client and HTTP request, configure Hawk filter.
     */
    Client client = ClientBuilder.newClient();
    WebTarget wt = client.target("http://www.example.org/api/context/news");
    wt.register(new ClientLoggingFilter("Sample client"));
    wt.register(hawkFilter);

    Response response = null;
    try {
        response = wt.request("*/*").get();
    } catch (HawkClockSkewTooLargeException e) {
        long offset;
        long clientNow = System.currentTimeMillis() / 1000L;
        long serverNow = e.getSuggestedTimestamp();
        provider.setClockOffset(serverNow - clientNow);
        // From here, retry request with corrected timestamp.
        // ...
    }
    if (response.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
        // Handle other 401 responses
        return;
    }



