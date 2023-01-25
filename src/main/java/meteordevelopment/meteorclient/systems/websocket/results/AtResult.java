package meteordevelopment.meteorclient.systems.websocket.results;

import meteordevelopment.meteorclient.systems.commands.commands.Check.GsonUtils;
import meteordevelopment.meteorclient.systems.websocket.Result;

public class AtResult extends Result {
    @Override
    public String toJSON() {
        return GsonUtils.beanToJson(get());
    }

    @Override
    public Result get() {
        return this;
    }
}
