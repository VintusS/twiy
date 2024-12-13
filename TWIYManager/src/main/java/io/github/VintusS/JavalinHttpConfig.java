package io.github.VintusS;

import io.github.VintusS.requests.UpdateLeaderRequest;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class JavalinHttpConfig {
    public static void setup(Javalin app) {
        app.post("/update_leader", JavalinHttpConfig::handleUpdateLeader);
    }

    public static void handleUpdateLeader(Context ctx) {
        UpdateLeaderRequest request = ctx.bodyAsClass(UpdateLeaderRequest.class);

        AddressHolder.getInstance().setHost(request.getLeaderHost());
        AddressHolder.getInstance().setPort(request.getLeaderPort());

        Main.logger.info("Changed host to leader: {}:{}", request.getLeaderHost(), request.getLeaderPort());
    }
}
