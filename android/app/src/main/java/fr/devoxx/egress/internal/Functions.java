package fr.devoxx.egress.internal;

import fr.devoxx.egress.model.Player;
import rx.functions.Func2;

public class Functions {

    public static Func2<String, String, Player> buildPlayer(final String mail) {
        return new Func2<String, String, Player>() {
            @Override
            public Player call(String token, String name) {
                return new Player(token, name, mail);
            }
        };
    }
}
