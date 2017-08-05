package app;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import mail.MessageSupervisor;
import model.SimpleRequestProtocol;

import java.io.IOException;
import java.util.UUID;

public class SimpleJavaMain {
    public static void main(String[] args) throws IOException {
        Config conf = ConfigFactory.load("application.conf");
        ActorSystem system = ActorSystem.create("akka-emailer");

        try {
            ActorRef supervisor = system.actorOf(MessageSupervisor.props(conf), "message-supervisor");

            SimpleRequestProtocol.RequestT msgWelcome = new SimpleRequestProtocol.Welcome(
                    UUID.randomUUID(), "obi-wan@rootservices.org", "obi-wan", "https://rootservices.org/verify"
            );
            supervisor.tell(msgWelcome, system.deadLetters());

            SimpleRequestProtocol.RequestT msgReset = new SimpleRequestProtocol.ResetPassword(
                    UUID.randomUUID(), "obi-wan@rootservices.org", "obi-wan", "https://rootservices.org/verify"
            );
            supervisor.tell(msgReset, system.deadLetters());

            System.out.println("Press ENTER to exit the system");
            System.in.read();

            supervisor.tell(PoisonPill.getInstance(), system.deadLetters());
        } finally {
            system.terminate();
        }
    }
}
