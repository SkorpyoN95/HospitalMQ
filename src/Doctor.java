import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Doctor {
    private final String name;
    private final String EXCHANGE_READ = "Doctors";
    private final String EXCHANGE_WRITE = "Technicians";
    private final String EXCHANGE_INFO = "Administrator";
    private final Connection connection;
    private final Channel channel;

    private Doctor(String name) throws Exception{
        this.name = name;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_READ, BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare(EXCHANGE_INFO, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        String infoQueue = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_READ, name);
        channel.queueBind(infoQueue, EXCHANGE_INFO, "");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                System.out.println("Received respond: " + message);
                channel.basicAck(envelope.getDeliveryTag(), false);
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

        channel.basicConsume(queueName, false, consumer);
        channel.basicConsume(infoQueue, false, info);
    }

    private void sendRequest(String patient, Injury injury) throws IOException {
        String message = name + "," + patient + "," + injury.toString();
        channel.basicPublish(EXCHANGE_WRITE, injury.toString(), null, message.getBytes("UTF-8"));
    }

    public static void main(String args[]) throws Exception {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Put doctor's name:");
        String name = br.readLine();
        Doctor doc = new Doctor(name);
        while(true){
            System.out.println("Enter new patient's name ('end' for quit):");
            String patient = br.readLine();
            if(patient.equals("end")) break;
            if(patient.equals("")) continue;

            System.out.println("Enter patient's injury [knee/hip/elbow]:");
            String inj = br.readLine();
            switch(inj){
                case "knee": doc.sendRequest(patient, Injury.KNEE); break;
                case "hip": doc.sendRequest(patient, Injury.HIP); break;
                case "elbow": doc.sendRequest(patient, Injury.ELBOW); break;
                default: System.out.println("Unknown type of injury!"); break;
            }
        }
        doc.channel.close();
        doc.connection.close();
    }
}
