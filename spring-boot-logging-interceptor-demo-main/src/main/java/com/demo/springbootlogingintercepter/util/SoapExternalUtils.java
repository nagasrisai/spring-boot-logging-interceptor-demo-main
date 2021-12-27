package com.demo.springbootlogingintercepter.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapBody;
import org.springframework.ws.soap.SoapEnvelope;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.soap.saaj.SaajSoapMessage;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;

import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;


@Slf4j
@ResponseStatus
public class SoapExternalUtils implements ClientInterceptor {


        @Override
        public boolean handleResponse(MessageContext messageContext) throws WebServiceClientException {
            StringBuilder responseBuilder = new StringBuilder();
                       try {

                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                messageContext.getResponse().writeTo(buffer);
                String payload = buffer.toString(StandardCharsets.UTF_8.name());
                TransportContext context = TransportContextHolder.getTransportContext();
                String url =context.getConnection().getUri().toString();
                URL obj=new URL(url);
                HttpURLConnection connection=(HttpURLConnection)obj.openConnection();

                responseBuilder.append("\n============================================ Inbound Soap RESPONSE ==================================================================================")
                        .append("\nResponse Status: "+ connection.getResponseCode())
                        .append("\nResponse Body: "+payload)
                        .append("\n===============================================================================================================================================");

                log.info(responseBuilder.toString());

            } catch (IOException |URISyntaxException e) {
                           log.error(e.getMessage());
            }

            return true;
        }
        

        @Override
        public boolean handleRequest(MessageContext messageContext) throws WebServiceClientException {
            StringBuilder requestBuilder  = new StringBuilder();

            try {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                messageContext.getRequest().writeTo(buffer);
                String payload = buffer.toString(StandardCharsets.UTF_8.name());

                TransportContext context = TransportContextHolder.getTransportContext();
                String url =context.getConnection().getUri().toString();
                requestBuilder.append("\n======================================== Outbound SOAP REQUEST =================================================================================")
                        .append("\nRequest Url:"+url)
                        .append("\nRequest Body: "+payload)
                        .append("\n===========================================================================================");
                log.info(requestBuilder.toString());
            } catch (IOException |URISyntaxException e) {
                log.error(e.getMessage());
            }

            return true;
        }

        @Override
        public boolean handleFault(MessageContext messageContext) throws WebServiceClientException {
            StringBuilder fault = new StringBuilder();
            fault.append("======================================== Inbound Soap Fault =================================================================================");
                WebServiceMessage message = messageContext.getResponse();
                SaajSoapMessage saajSoapMessage = (SaajSoapMessage)message;
                SOAPMessage soapMessage = saajSoapMessage.getSaajMessage();
                SOAPPart soapPart = soapMessage.getSOAPPart();
                try {
                    SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
                    SOAPBody soapBody = soapEnvelope.getBody();
                    SOAPFault soapFault = soapBody.getFault();
                    log.info(fault.toString());
                    log.error(String.format(soapFault.getFaultString()));
                    log.info(fault.toString());

                } catch (SOAPException e) {
                log.error(e.getMessage());
            }
            return true;

        }

    @Override
    public void afterCompletion(MessageContext messageContext, Exception e) throws WebServiceClientException {

    }
    private SoapBody getSoapBody(MessageContext messageContext) {
        SoapMessage soapMessage = (SoapMessage) messageContext.getResponse();
        SoapEnvelope soapEnvelope = soapMessage.getEnvelope();


        return soapEnvelope.getBody();
    }


}
