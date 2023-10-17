package jadelab1;

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.io.*;
import java.net.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

public class StandAloneAgent extends Agent {
    protected void setup() {

        //services registration at DF
        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());

        //service merged
        ServiceDescription sd = new ServiceDescription();
        sd.setType("dictionary"); // default = dictionary
        dfad.addServices(sd);

        try {
            DFService.register(this, dfad);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }

        addBehaviour(new DictionaryCyclicBehaviour(this));
    }

    protected void takeDown() {
        //services deregistration before termination
        try {
            DFService.deregister(this);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }
    }

    public String makeRequest(String serviceName, String word) {
        StringBuffer response = new StringBuffer();
        try {
            URL url = new URL("http://dict.org/bin/Dict");
            URLConnection urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String content = "Form=Dict1&Strategy=*&Database=" + URLEncoder.encode(serviceName) + "&Query=" + URLEncoder.encode(word) + "&submit=Submit+query";

            //forth
            DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
            printout.writeBytes(content);
            printout.flush();
            printout.close();

            //back
            DataInputStream input = new DataInputStream(urlConn.getInputStream());
            String str;
            while (null != ((str = input.readLine()))) {
                response.append(str);
            }
            input.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        //cut what is unnecessary
        return response.substring(response.indexOf("<hr>") + 4, response.lastIndexOf("<hr>"));
    }

    class DictionaryCyclicBehaviour extends CyclicBehaviour {
        StandAloneAgent agent;

        public DictionaryCyclicBehaviour(StandAloneAgent agent) {
            this.agent = agent;
        }

        public void action() {

            MessageTemplate template = MessageTemplate.MatchOntology("dictionary");
            ACLMessage message = agent.receive(template);

            if (message == null) {
                block();
            } else {
                //process the incoming message
                String content = message.getContent();
                ACLMessage reply = message.createReply();
                reply.setPerformative(ACLMessage.INFORM);
                String response = "";

                //output message
                try {
                    response = agent.makeRequest(content, content);
                } catch (NumberFormatException ex) {
                    response = ex.getMessage();
                }

                reply.setContent(response);
                agent.send(reply);
            }
        }
    }
}
