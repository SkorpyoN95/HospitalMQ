import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Administrator {
    private List<Log> logs = new ArrayList<>();
    private final String EXCHANGE_ONE = "Doctors";
    private final String EXCHANGE_TWO = "Technicians";
    private final String EXCHANGE_INFO = "Administrator";
    private final Connection connection;
    private final Channel channel;

    public Administrator() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_ONE, BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare(EXCHANGE_TWO, BuiltinExchangeType.TOPIC);
        channel.exchangeDeclare(EXCHANGE_INFO, BuiltinExchangeType.FANOUT);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_ONE, "#");
        channel.queueBind(queueName, EXCHANGE_TWO, "#");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body, "UTF-8");
                String[] params = message.split(",");
                Log new_log;
                if(params[2].equals("done")) {
                    new_log = new Log("respond", LocalDateTime.now().toString(), message);
                }else{
                    new_log = new Log("request (dr. " + params[0] + ")", LocalDateTime.now().toString(), message.substring(message.indexOf(",") + 1));
                }
                logs.add(new_log);
                new_log.print();
                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        channel.basicConsume(queueName, false, consumer);
    }

    private void sendInfo(String info) throws IOException {
        channel.basicPublish(EXCHANGE_INFO, "", null, info.getBytes("UTF-8"));
        Log new_log = new Log("info", LocalDateTime.now().toString(), info);
        logs.add(new_log);
        new_log.print();
    }

    public static void main(String args[]) throws Exception {
        Administrator admin = new Administrator();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            System.out.println("Put message for everyone: ");
            String text = br.readLine();
            if(text.equals("end")) break;
            if(text.equals("")) continue;
            admin.sendInfo(text);
        }
        admin.channel.close();
        admin.connection.close();
    }
}
