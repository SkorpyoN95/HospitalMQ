import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

public class Technician {
    private final Injury injury1;
    private final Injury injury2;
    private final String EXCHANGE_READ = "Technicians";
    private final String EXCHANGE_WRITE = "Doctors";
    private final String EXCHANGE_INFO = "Administrator";
    private final Channel channel;

    private Technician(Injury injury1, Injury injury2) throws IOException, TimeoutException {
        this.injury1 = injury1;
        this.injury2 = injury2;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_READ, BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare(EXCHANGE_INFO, BuiltinExchangeType.FANOUT);

        channel.queueDeclare(injury1.toString(), false, false, false, null);
        channel.queueDeclare(injury2.toString(), false, false, false, null);
        String infoQueue = channel.queueDeclare().getQueue();

        channel.queueBind(injury1.toString(), EXCHANGE_READ, injury1.toString());
        channel.queueBind(injury2.toString(), EXCHANGE_READ, injury2.toString());
        channel.queueBind(infoQueue, EXCHANGE_INFO, "");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("I got request for examination!");
                String[] params = message.split(",");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String response = params[1] + "," + params[2] + ",done";
                channel.basicPublish(EXCHANGE_WRITE, params[0], null, response.getBytes("UTF-8"));
                channel.basicAck(envelope.getDeliveryTag(), false);
                System.out.println("Done!");
            }
        };

        Consumer info = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Information from administration: " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        channel.basicConsume(injury1.toString(), false, consumer);
        channel.basicConsume(injury2.toString(), false, consumer);
        channel.basicConsume(infoQueue, false, info);
    }

    public static void main(String args[]) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        Injury spec1, spec2;
        do{
            System.out.println("Put technician's specialization[knee/hip/elbow]:");
            String inj = br.readLine();
            switch(inj){
                case "knee": spec1 = Injury.KNEE; break;
                case "hip": spec1 =  Injury.HIP; break;
                case "elbow": spec1 = Injury.ELBOW; break;
                default: System.out.println("Unknown type of injury!"); spec1 = null; break;
            }
        }while(spec1 == null);
        do{
            System.out.println("Put technician's second specialization[knee/hip/elbow]:");
            String inj = br.readLine();
            switch(inj){
                case "knee": spec2 = Injury.KNEE; break;
                case "hip": spec2 =  Injury.HIP; break;
                case "elbow": spec2 = Injury.ELBOW; break;
                default: System.out.println("Unknown type of injury!"); spec2 = null; break;
            }
        }while(spec2 == null || spec1 == spec2);
        Technician tech = new Technician(spec1, spec2);
    }
}
