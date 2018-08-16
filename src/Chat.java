import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import java.io.*;
import java.util.List;
import java.util.LinkedList;

public class Chat extends ReceiverAdapter {
    JChannel channel;
    String user_name=System.getProperty("user.name", "n/a");
    final List<String> state=new LinkedList<>();
   
    /*
    Esse método pe chamado sempre que um novo usuário entra no chat ou algum sai
    Ele é o responsável por imprimir as informações da sala
    */
    public void viewAccepted(View new_view) {
         System.out.println("" + new_view);
    }
    /*
    Esse método é utilizado para receber as mensagens
    */
    public void receive(Message msg) {
        String line= "" + msg.getObject();
        System.out.println(line);
        synchronized(state) {
            state.add(line);
        }
    }

    public void getState(OutputStream output) throws Exception {
        synchronized(state) {
            Util.objectToStream(state, new DataOutputStream(output));
        }
    }

    @SuppressWarnings("unchecked")
    public void setState(InputStream input) throws Exception {
        List<String> list=Util.objectFromStream(new DataInputStream(input));
        synchronized(state) {
            state.clear();
            state.addAll(list);
        }
        System.out.println("(" + list.size() + " histórico de mensagens):");
        list.forEach(System.out::println);
    }

/*
    O JChannel é utilizado para entrar no cluster, uma instância do Jchannel é criada
    com uma configuração que define as propriedades do canal.
    O método connect é utilizado para realizar a conexão com o cluster.
    Sendo assim, todas as instâncias que chamam connect utilizando o nome do cluster se juntarão
    ao mesmo cluster.
    Uma propriedade importante do método conect é que ele cria a primeira instancia do cluster se 
    a mesma não tiver sido criada previamente, e se já tiver ocorrido a criação da primeira instância
    as outras instancias se juntam ao cluster criado.
    
    */
    private void start() throws Exception {
        //ProtocolStack stack=new ProtocolStack();
        channel=new JChannel("udp.xml").setReceiver(this);
        channel.connect("ChatCluster");
        channel.getState(null, 10000);
        
        //String info = channel.getProperties();
        //System.out.println("Informações do protocolo: "+info);
        eventLoop();
        channel.close();
    }
/*
    Nesse método é lido o nome do usuário assim que ele entra no chat e 
    enviada a sua mensagem para todos do grupo.
    Caso a mensagem sair seja digitada o canal é fechado
    A mensagem é enviada utilizando channel.send
    Em mensagem o argumento nulo indica que a mensagem será enviada para todos os membros do grupo
    
    */
    private void eventLoop() throws IOException {
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        BufferedReader nome=new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Digite seu nome: "); System.out.flush();
        String name=nome.readLine();
        while(true) {
            try {
                System.out.print(">> "); System.out.flush();
                String line=in.readLine().toLowerCase();
                if(line.startsWith("sair") || line.startsWith("exit")) {
                    System.out.print("Voce foi desconectado");
                    break;
                }
                line="[" + name + "] " +" diz: "+ line;
                Message msg=new Message(null, line);
                channel.send(msg);
            }
            catch(Exception e) {
            }
        }
    }


    public static void main(String[] args) throws Exception {
       System.out.println("Bem vindo ao Chat");
        new Chat().start();
    }
}