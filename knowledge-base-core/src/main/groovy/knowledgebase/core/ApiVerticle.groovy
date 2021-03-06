package knowledgebase.core

import io.vertx.lang.groovy.GroovyVerticle
import io.vertx.core.Future
import io.vertx.groovy.core.http.HttpServer
import io.vertx.groovy.ext.web.Router
import io.vertx.groovy.core.Vertx
import knowledgebase.core.api.resource.InstanceResource
import knowledgebase.core.api.resource.MetadataContextResource
import knowledgebase.core.storage.Storage
import knowledgebase.core.util.WebRequestDiagnostics
import knowledgebase.core.api.resource.RootResource

import java.util.concurrent.CompletableFuture

public class ApiVerticle extends GroovyVerticle {

    private HttpServer server;

    public static void deploy(Vertx vertx, CompletableFuture deployed) {
        vertx.deployVerticle("groovy:knowledgebase.core.ApiVerticle", { res ->
            if (res.succeeded()) {
                deployed.complete(null);
            } else {
                deployed.completeExceptionally(res.cause());
            }
        });
    }

    public static CompletableFuture<Void> deploy(Vertx vertx) {
        def deployed = new CompletableFuture()

        deploy(vertx, deployed)

        deployed
    }

    @Override
    public void start(Future deployed) {
        server = vertx.createHttpServer()

        def router = Router.router(vertx)

        router.route().handler(WebRequestDiagnostics.&outputDiagnostics)

        RootResource.register(router)
        MetadataContextResource.register(router)
        InstanceResource.register(router, Storage.collectionProvider.instanceCollection)

        server.requestHandler(router.&accept)
                .listen(9401,
                { result ->
                    if (result.succeeded()) {
                        printf "Listening on %s \n", server.actualPort()
                        deployed.complete();
                    } else {
                        deployed.fail(result.cause());
                    }
                })
    }

    @Override
    public void stop(Future stopped) {
        server.close({ result ->
            if (result.succeeded()) {
                println "Stopped listening"
                stopped.complete();
            } else {
                stopped.fail(result.cause());
            }
        });
    }
}
