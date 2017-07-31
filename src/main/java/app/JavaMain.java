package app;

import akka.actor.ActorSystem;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import mq.RMQActor;

import java.io.IOException;

public class JavaMain {
    public static void main(String[] args) throws IOException {
        Config conf = ConfigFactory.load("application.conf");
        ActorSystem system = ActorSystem.create("akka-emailer");
        try {
            system.actorOf(RMQActor.props(conf), "rmq-listener");

            System.out.println("Press ENTER to exit the system");
            System.in.read();
        } finally {
            system.terminate();
        }
    }
}
