package im.jxl.bm;

import im.jxl.bm.model.Message;
import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class App extends AbstractVerticle implements Handler<HttpServerRequest> {
    private static Logger logger = LoggerFactory.getLogger(App.class.getName());
    private static final int DEFAULT_PORT = 8080;
    private static final String CONFIG_KEY_PORT = "port";

    private static final CharSequence RESPONSE_TYPE_PLAIN = HttpHeaders.createOptimized("text/plain");
    private static final CharSequence RESPONSE_TYPE_HTML = HttpHeaders.createOptimized("text/html; charset=UTF-8");
    private static final CharSequence RESPONSE_TYPE_JSON = HttpHeaders.createOptimized("application/json");

    private static final CharSequence HEADER_SERVER = HttpHeaders.createOptimized("server");
    private static final CharSequence HEADER_DATE = HttpHeaders.createOptimized("date");
    private static final CharSequence HEADER_CONTENT_TYPE = HttpHeaders.createOptimized("content-type");
    private static final CharSequence HEADER_CONTENT_LENGTH = HttpHeaders.createOptimized("content-length");

    private static final CharSequence SERVER = HttpHeaders.createOptimized("vert.x");

    private static final String HELLO_WORLD = "Hello, world!";
    private static final Buffer HELLO_WORLD_BUFFER = Buffer.factory.directBuffer(HELLO_WORLD, "UTF-8");
    private static final CharSequence HELLO_WORLD_LENGTH = HttpHeaders.createOptimized("" + HELLO_WORLD.length());

    private static final String PATH_PLAINTEXT = "/plaintext";
    private static final String PATH_JSON = "/json";

    private HttpServer server;
    private CharSequence dateString;
    private CharSequence[] plaintextHeaders;

    public static CharSequence createDateHeader() {
        return HttpHeaders.createOptimized(DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
    }

    @Override
    public void start() throws Exception {
        JsonObject config = config();

        server = vertx.createHttpServer(new HttpServerOptions());
        server.requestHandler(App.this).listen(config.getInteger(CONFIG_KEY_PORT, DEFAULT_PORT));
        dateString = createDateHeader();

        plaintextHeaders = new CharSequence[] {
                HEADER_CONTENT_TYPE, RESPONSE_TYPE_PLAIN,
                HEADER_SERVER, SERVER,
                HEADER_DATE, dateString,
                HEADER_CONTENT_LENGTH, HELLO_WORLD_LENGTH };

        vertx.setPeriodic(1000, id -> plaintextHeaders[5] = dateString = createDateHeader());

//        PgPoolOptions options = new PgPoolOptions();
//        options.setDatabase(config.getString("database"));
//        options.setHost(config.getString("host"));
//        options.setPort(config.getInteger("port", 5432));
//        options.setUser(config.getString("username"));
//        options.setPassword(config.getString("password"));
//        options.setCachePreparedStatements(true);
//        client = PgClient.pool(vertx, new PgPoolOptions(options).setMaxSize(1));
//        pool = PgClient.pool(vertx, new PgPoolOptions(options).setMaxSize(4));
    }

    @Override
    public void stop() {
        if (server != null) server.close();
    }

    @Override
    public void handle(HttpServerRequest request) {
        switch (request.path()) {
            case PATH_PLAINTEXT:
                handlePlainText(request);
                break;
            case PATH_JSON:
                handleJson(request);
                break;
            default:
                request.response().setStatusCode(404);
                request.response().end();
                break;
        }
    }

    private void handleJson(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        MultiMap headers = response.headers();
        headers
                .add(HEADER_CONTENT_TYPE, RESPONSE_TYPE_JSON)
                .add(HEADER_SERVER, SERVER)
                .add(HEADER_DATE, dateString);
        Message message = new Message();
        message.setMessage("Hello, World!");
        response.end(message.toBuffer());
    }

    private void handlePlainText(HttpServerRequest request) {
        HttpServerResponse response = request.response();
        MultiMap headers = response.headers();
        for (int i = 0;i < plaintextHeaders.length; i+= 2) {
            headers.add(plaintextHeaders[i], plaintextHeaders[i + 1]);
        }
        response.end(HELLO_WORLD_BUFFER);
    }

    public static void main(String[] args) throws Exception {
        JsonObject config = new JsonObject();

        InputStream inputStream = null;
        try {
            inputStream = App.class.getClassLoader().getResourceAsStream("vertx_config.json");
            byte[] buf = new byte[inputStream.available()];
            inputStream.read(buf);
            config = new JsonObject(new String(buf, "UTF-8"));
        }catch (Exception ex) {
            logger.error("read config failed.", ex);
        }finally {
            if(inputStream != null) {
                inputStream.close();
            }
        }

        Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
        vertx.exceptionHandler(err -> {
            logger.error("vertx exception", err);
        });
        printConfig(vertx);

        final int port = config.getInteger(CONFIG_KEY_PORT, DEFAULT_PORT);
        vertx.deployVerticle(App.class.getName(),
                new DeploymentOptions().setInstances(
                        VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE).setConfig(config), event -> {
                    if (event.succeeded()) {
                        logger.info("Server listening on port " + port);
                    } else {
                        logger.error("Unable to start your application", event.cause());
                    }
                });
    }

    private static void printConfig(Vertx vertx) {
        boolean nativeTransport = vertx.isNativeTransportEnabled();
        String version = "unknown";
        try {
            InputStream in = Vertx.class.getClassLoader().getResourceAsStream("META-INF/vertx/vertx-version.txt");
            if (in == null) {
                in = Vertx.class.getClassLoader().getResourceAsStream("vertx-version.txt");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            while (true) {
                int amount = in.read(buffer);
                if (amount == -1) {
                    break;
                }
                out.write(buffer, 0, amount);
            }
            version = out.toString();
        } catch (IOException e) {
            logger.error("Could not read Vertx version", e);;
        }
        logger.info("Vertx: " + version);
        logger.info("Event Loop Size: " + VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE);
        logger.info("Native transport : " + nativeTransport);
    }
}
