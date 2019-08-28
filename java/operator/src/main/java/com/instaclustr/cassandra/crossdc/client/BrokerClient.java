package com.instaclustr.cassandra.crossdc.client;

import com.instaclustr.cassandra.operator.k8s.K8sResourceUtils;
import com.instaclustr.cassandra.operator.model.Seed;
import com.instaclustr.cassandra.operator.model.SeedList;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreApi;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.KubeConfig;
import io.kubernetes.client.util.credentials.AccessTokenAuthentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * A Broker Client
 */
@Singleton
public class BrokerClient {

    private static String API;
    private ApiClient apiClient;
    public static String NAMESPACE;
    private static String CA;
    private static String TOKEN;
    private static final Logger logger = LoggerFactory.getLogger(BrokerClient.class);

    @Inject
    public BrokerClient() {

        CA = System.getenv("CA");
        TOKEN = System.getenv("TOKEN");
        NAMESPACE = System.getenv("NAMESPACE");
        API = System.getenv("API");
        logger.info("CA {} \n, TOKEN {} \n NAMESPACE {} \n API {}", CA, TOKEN, NAMESPACE, API);
        if (CA == null || TOKEN == null || NAMESPACE == null) {
            throw new IllegalStateException(String.format("CA %s , TOKEN %s , NAMESPACE %s requires \"broker client\" argument.", CA, TOKEN, NAMESPACE));
        }

        try {
            apiClient = new ClientBuilder()
                    .setBasePath(API)
                    .setCertificateAuthority(Base64.getDecoder().decode(CA))
                    .setAuthentication(new AccessTokenAuthentication(TOKEN))
                    .setVerifyingSsl(true)
                    .setOverridePatchFormat("application/json-patch+json")
                    .build();
            apiClient.getHttpClient().setReadTimeout(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ApiClient getBrokerClient() {
        return this.apiClient;
    }
//
//    public static void main(String[] args) throws ApiException {
//        ApiClient apiClient = new ClientBuilder()
//                .setBasePath("https://54.222.151.123:6443")
//                .setCertificateAuthority(Base64.getDecoder().decode("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUN3akNDQWFxZ0F3SUJBZ0lCQURBTkJna3Foa2lHOXcwQkFRc0ZBREFTTVJBd0RnWURWUVFERXdkcmRXSmwKTFdOaE1CNFhEVEU1TURneU1EQXhOVFF6T0ZvWERUSTVNRGd4TnpBeE5UUXpPRm93RWpFUU1BNEdBMVVFQXhNSAphM1ZpWlMxallUQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCQUtpVkxDSyt2NUVuCmdBRTE4TytpVGZ4OW9UTGNxSHNTeGVHN2QzMWN5NGhITk84aEM3MXR6UVFPMmY5RzRPMWZ1VWZBWXRtWkJQM1IKaHl6a0RhL0VpLzh0OUFHQ3JXdktIQzgvTjJReHRZeGhQOFUrZ0ZOZE45emJqbW5mWno0bU5RVlVXTzQ4OXVlWQpHdDRXbVk5Y2l4RzlkaG1Sd014L2hCbVlSOTVBb1lmWDcyOSsvVTN6bXlwTDhrd3ROUWpkRkFHL0l3ZEUxMkdkCjFGS3B3SWs5TWdnOUZ6b2QyMjB3NXpobm1Ud2FXcm1KMnFrUld5cEhLU1ZpelUrckgrNkxwYXdjbGp6TElRS2IKaE8ra3hoby9DSzRSZmRpdUhkNWErZDIxMFRsZEY4RjFHMFJBRnZObzRGMlBpRG1jeDRxNWlERUtxUlpUczVtKwplVGtRMFEzRHJua0NBd0VBQWFNak1DRXdEZ1lEVlIwUEFRSC9CQVFEQWdLa01BOEdBMVVkRXdFQi93UUZNQU1CCkFmOHdEUVlKS29aSWh2Y05BUUVMQlFBRGdnRUJBR3BPZzFwaVdiTEVkNWZLb0gyZzFnRDNxNk03WjB0dXZiZkcKUUZZcERteUpXZFgyU01NNEVFMkg0aVgwMVUvZ0g3YkphS2Fjc3hiaFVVOHd1YWZZWTNuVkNXV2QraTdYNnBYZgo5RjJuQ05WYVdkZ21TQWJjK0pHSTJtV2lNc1lMcWtqVittZEdGSHduRmh4b2I2aUIwMG9KNk1xZHhrcktKNWVxCk41RVRheXJZZ3RHWkUyNUM5TFo3YnV2VVFLZURwckZQNXA4NUNnR3pSNFg4T09wVUoyWFZ1WC9ZQzdoY3h3bnMKN1dWYVlXOW1VQU1NRWlPZ3c5RDZvKythcDc1cCt6TnBoSkVoV1Bza0IvcERtTEVqVFlQSzg2RHArS2tFYXFMSwpUMzF6SWdlQWdkK3dvdmFGYW5oaFJDbm5zZ0VGSUhwSloxbyt6K1p3K1hxbGswTk5ybXc9Ci0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0K"))
//                .setAuthentication(new AccessTokenAuthentication("eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJjYXNzYW5kcmEtb3BlcmF0b3ItYnJva2VyIiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZWNyZXQubmFtZSI6ImNhc3NhbmRyYS1vcGVyYXRvci1icm9rZXItY2xpZW50LXRva2VuLW02bGY3Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQubmFtZSI6ImNhc3NhbmRyYS1vcGVyYXRvci1icm9rZXItY2xpZW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9zZXJ2aWNlLWFjY291bnQudWlkIjoiOTZhYjljNzktYzRiNS0xMWU5LTg1MTItMDZhODA4OGNhZWI2Iiwic3ViIjoic3lzdGVtOnNlcnZpY2VhY2NvdW50OmNhc3NhbmRyYS1vcGVyYXRvci1icm9rZXI6Y2Fzc2FuZHJhLW9wZXJhdG9yLWJyb2tlci1jbGllbnQifQ.K1Zzjq2lRjNGgFkHLVzXenk-uWR53_NIrJVp1QnH95TQxTR_MRUosJ_ZVoV0j_BfJ7D3-1ah-Q513bdIt3KJIcIZjraa2zcuHzr8lG8XSUDUqw7qkd8KQ9O_w3nR2jongbK9h04ArDzDy99irKs4LRC_LMcg6WUwcotL26YjZPYrKtncpkba1rsPgWgeoQLWdBfHZvVSRxFDXMvCSfKvU1JyFjc5L2Lx5DlwCP8dABYLhNO8c6_hsi-16kmoAqL8E-EaPNA50TsB5yStVvuVzClTdgVjhyst6Qms3jlZ7R0rwUrcuoQW5eyn49GHCnw0tLJ7ADlrli5GxIb6h4yXmg"))
//                .setVerifyingSsl(true)
//                .setOverridePatchFormat("application/json-patch+json")
//                .build();
//        CoreV1Api coreApi = new CoreV1Api(apiClient);
//        System.out.println(coreApi.listNamespace(true, null, null,null,null,null,null,null,false));
//
//    }
}
