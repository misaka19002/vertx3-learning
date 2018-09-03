package com.rainday.app

import io.vertx.core.AbstractVerticle
import io.vertx.core.Context
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.streams.Pump
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import java.time.LocalDateTime

/**
 * Created by wyd on 2017/11/10 11:18:32.
 */
class HttpFileServerVerticle : AbstractVerticle() {
    val mainRouter = Router.router(vertx)
    val httpClient by lazy { vertx.createHttpClient(HttpClientOptions().setKeepAlive(false)) }
    val webClient by lazy { WebClient.wrap(httpClient) }


    override fun init(vertx: Vertx, context: Context) {
        super.init(vertx, context)

        mainRouter.route("/file/upload/:urlTag").method(HttpMethod.POST).handler(this::fileHandler)
    }

    override fun start() {
        try {
            vertx.createHttpServer().requestHandler(mainRouter::accept).listen(8081)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 本方法将vertx收到的请求直接转给后端服务器
     * 注意各个handler之间的顺序，开发过程中遇到了很多问题。都是由于对httpclient以及httpclientrequest中的各个handler执行顺序不熟悉导致。
     * 建议多看官方javadoc详细了解，这里仅做一些介绍。
     * 1、fileHandler是vertx在收到前端的请求立马执行的。
     * 2、这里我们首先将收到的请求中的header，设置到httpclientrequest中。然后用httpclientrequest将收到的流pump到后台。
     * 3、httpclientrequest的handler方法：注册一个方法，当request有响应时，则调用此方法。
     * 4、httpclientrequest的endhandler方法：一旦数据流执行读取完毕则调用此方法，所以这里我们在数据读取完之后调用httpclientrequest的end方法。告知后端服务器请求已经发送完毕。
     * 5、httpclientresponse的endhandler方法：当后端服务器的响应完毕则调用此方法。此时我们需要做的就是调用浏览器与vertx的响应流，用以告知浏览器响应结束。
     * ##
     * 第4步的httpclientrequest.end()方法一定要调用。这个很重要。如果不调用会导致vertx抛出异常。在httpclientrequest 超时需要关闭的时候抛出connection was closed。
     * 原因就是clientrequest没有调用end方法，告诉后台服务器httpclientrequest已经完成数据发送。
     * ##
     */
    fun fileHandler(context: RoutingContext) {
        val request = context.request()
        val response = context.response()
        val urlTag = request.getParam("urlTag")
        request.setExpectMultipart(true)
//        val clientRequest = httpClient.requestAbs(HttpMethod.POST, "http://127.0.0.1:8877/file/upload/$urlTag")
        val clientRequest = httpClient.request(HttpMethod.POST, 8877, "127.0.0.1", "/file/upload/${urlTag}").setTimeout(60 * 1000)
        println(Json.encodePrettily(request.headers().entries()))

        println("header start ${LocalDateTime.now()}")
        clientRequest.headers().addAll(request.headers())

        println("header end ${LocalDateTime.now()}")

        println("pump file start ${LocalDateTime.now()}")
        Pump.pump(request, clientRequest).start()
        println("pump file end ${LocalDateTime.now()}")
        clientRequest.handler {
            println("clientRequest status ${clientRequest.connection()}")
            clientRequest.connection().closeHandler {
                println("connection closed ")
            }
            println("clientRequest status ${clientRequest.connection()}")

            println("receive response ${LocalDateTime.now()}")
            println(Json.encodePrettily(it.headers().entries()))
            response.headers().addAll(it.headers())

            println("pump ${LocalDateTime.now()}")
            Pump.pump(it, response).start()

            it.endHandler {
                println("response endhandler ${LocalDateTime.now()}")
                response.end()
            }
        }

        clientRequest.endHandler {
            println("client endhandler ${LocalDateTime.now()}")
            clientRequest.end()
        }
    }
}
