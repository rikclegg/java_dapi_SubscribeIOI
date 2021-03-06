/* Copyright 2013. Bloomberg Finance L.P.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:  The above
 * copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.bloomberg.ioi.samples;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Datetime;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.EventHandler;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Subscription;
import com.bloomberglp.blpapi.SubscriptionList;

public class Java_dapi_SubscribeIOI {
	
    private static final Name 	IOI_DATA = new Name("Ioidata");

	// ADMIN
	private static final Name 	SLOW_CONSUMER_WARNING	= new Name("SlowConsumerWarning");
	private static final Name 	SLOW_CONSUMER_WARNING_CLEARED	= new Name("SlowConsumerWarningCleared");

	// SESSION_STATUS
	private static final Name 	SESSION_STARTED 		= new Name("SessionStarted");
	private static final Name 	SESSION_TERMINATED 		= new Name("SessionTerminated");
	private static final Name 	SESSION_STARTUP_FAILURE = new Name("SessionStartupFailure");
	private static final Name 	SESSION_CONNECTION_UP 	= new Name("SessionConnectionUp");
	private static final Name 	SESSION_CONNECTION_DOWN	= new Name("SessionConnectionDown");

	// SERVICE_STATUS
	private static final Name 	SERVICE_OPENED 			= new Name("ServiceOpened");
	private static final Name 	SERVICE_OPEN_FAILURE 	= new Name("ServiceOpenFailure");

	// SUBSCRIPTION_STATUS + SUBSCRIPTION_DATA
	private static final Name	SUBSCRIPTION_FAILURE 	= new Name("SubscriptionFailure");
	private static final Name	SUBSCRIPTION_STARTED	= new Name("SubscriptionStarted");
	private static final Name	SUBSCRIPTION_TERMINATED	= new Name("SubscriptionTerminated");
	
	private Subscription 		ioiSubscription;
	
	private String 				d_service;
    private String            	d_host;
    private int               	d_port;
    
    public static void main(String[] args) throws java.lang.Exception
    {
        System.out.println("Bloomberg - IOI API Example - DesktopAPI - IOISubscribe\n");
        System.out.println("Press ENTER at anytime to quit");

        Java_dapi_SubscribeIOI example = new Java_dapi_SubscribeIOI();
        example.run(args);

        System.in.read();    
    }
    
    public Java_dapi_SubscribeIOI()
    {
    
    	// Define the service required
    	
    	d_service = "//blp/ioisub-beta";
    	d_host = "localhost";
        d_port = 8194;

    }

    private void run(String[] args) throws Exception
    {

        SessionOptions d_sessionOptions = new SessionOptions();
        d_sessionOptions.setServerHost(d_host);
        d_sessionOptions.setServerPort(d_port);
        
        Session session = new Session(d_sessionOptions, new IOIEventHandler());
        
        session.startAsync();

    }

    class IOIEventHandler implements EventHandler
    {
        public void processEvent(Event event, Session session)
        {
            try {
                switch (event.eventType().intValue())
                {                
                case Event.EventType.Constants.ADMIN:
                    processAdminEvent(event, session);
                    break;
                case Event.EventType.Constants.SESSION_STATUS:
                    processSessionEvent(event, session);
                    break;
                case Event.EventType.Constants.SERVICE_STATUS:
                    processServiceEvent(event, session);
                    break;
                case Event.EventType.Constants.SUBSCRIPTION_DATA:
                    processSubscriptionDataEvent(event, session);
                    break;
                case Event.EventType.Constants.SUBSCRIPTION_STATUS:
                    processSubscriptionStatus(event, session);
                    break;
                default:
                    processMiscEvents(event, session);
                    break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

		private boolean processAdminEvent(Event event, Session session) throws Exception 
		{
            System.out.println("Processing " + event.eventType().toString());
        	MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                if(msg.messageType().equals(SLOW_CONSUMER_WARNING)) {
                	System.err.println("Warning: Entered Slow Consumer status");
                } else if(msg.messageType().equals(SLOW_CONSUMER_WARNING_CLEARED)) {
                	System.out.println("Slow consumer status cleared");
                	return false;
                }
            }
            return true;
		}

		private boolean processSessionEvent(Event event, Session session) throws Exception 
		{
            System.out.println("Processing " + event.eventType().toString());
        	MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                if(msg.messageType().equals(SESSION_STARTED)) {
                	System.out.println("Session started...");
                	session.openServiceAsync(d_service);
                } else if(msg.messageType().equals(SESSION_STARTUP_FAILURE)) {
                	System.err.println("Error: Session startup failed");
                	return false;
                } else if(msg.messageType().equals(SESSION_TERMINATED)) {
                	System.err.println("Session has been terminated");
                	return false;
                } else if(msg.messageType().equals(SESSION_CONNECTION_UP)) {
                	System.out.println("Session connection is up");
                } else if(msg.messageType().equals(SESSION_CONNECTION_DOWN)) {
                	System.err.println("Session connection is down");
                	return false;
                }
            }
            return true;
		}

        private boolean processServiceEvent(Event event, Session session) 
        {
            System.out.println("Processing " + event.eventType().toString());
        	MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                if(msg.messageType().equals(SERVICE_OPENED)) {
                	
                	System.out.println("Service opened...");

                	createSubscription(session);

                } else if(msg.messageType().equals(SERVICE_OPEN_FAILURE)) {
                	System.err.println("Error: Service failed to open");
                	return false;
                }
            }
            return true;
		}

		private boolean processSubscriptionStatus(Event event, Session session) throws Exception 
		{
            System.out.println("Processing " + event.eventType().toString());
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                if(msg.messageType().equals(SUBSCRIPTION_STARTED)) {
            		System.out.println("Subscription started successfully");
                } else if(msg.messageType().equals(SUBSCRIPTION_FAILURE)) {
            		System.err.println("Error: Subscription failed");
                    System.out.println("MESSAGE: " + msg);
                	return false;
                } else if(msg.messageType().equals(SUBSCRIPTION_TERMINATED)) {
            		System.err.println("Subscription terminated");
                    System.out.println("MESSAGE: " + msg);
                	return false;
                }
            }
            return true;
        }

        private boolean processSubscriptionDataEvent(Event event, Session session) throws Exception
        {

        	MessageIterator msgIter = event.messageIterator();
            
        	while (msgIter.hasNext()) {
            
        		Message msg = msgIter.next();
                
        		System.out.println("Message: " + msg.toString());
        		
                // Processing the field values in the subscription data...
                
        		if(msg.messageType().equals(IOI_DATA)) {

       				System.out.println("Processing " + event.eventType().toString());
 
					String change = msg.hasElement("change") ? msg.getElementAsString("change") : "";
					String id_value = msg.hasElement("id_value") ? msg.getElementAsString("id_value") : "";
					String originalId_value = msg.hasElement("originalId_value") ? msg.getElementAsString("originalId_value") : "";
					String ioi_bid_notes = msg.hasElement("ioi_bid_notes") ? msg.getElementAsString("ioi_bid_notes") : "";
					String ioi_bid_price_fixed_currency = msg.hasElement("ioi_bid_price_fixed_currency") ? msg.getElementAsString("ioi_bid_price_fixed_currency") : "";
					Double ioi_bid_price_fixed_price = msg.hasElement("ioi_bid_price_fixed_price") ? msg.getElementAsFloat64("ioi_bid_price_fixed_price") : 0;
					Double ioi_bid_price_moneyness = msg.hasElement("ioi_bid_price_moneyness") ? msg.getElementAsFloat64("ioi_bid_price_moneyness") : 0;
					Double ioi_bid_price_pegged_limitPrice = msg.hasElement("ioi_bid_price_pegged_limitPrice") ? msg.getElementAsFloat64("ioi_bid_price_pegged_limitPrice") : 0;
					Double ioi_bid_price_pegged_offsetAmount = msg.hasElement("ioi_bid_price_pegged_offsetAmount") ? msg.getElementAsFloat64("ioi_bid_price_pegged_offsetAmount") : 0;
					String ioi_bid_price_pegged_offsetFrom = msg.hasElement("ioi_bid_price_pegged_offsetFrom") ? msg.getElementAsString("ioi_bid_price_pegged_offsetFrom") : "";
					String ioi_bid_price_reference = msg.hasElement("ioi_bid_price_reference") ? msg.getElementAsString("ioi_bid_price_reference") : "";
					String ioi_bid_price_type = msg.hasElement("ioi_bid_price_type") ? msg.getElementAsString("ioi_bid_price_type") : "";
					String ioi_big_qualifiers_0 = msg.hasElement("ioi_big_qualifiers_0") ? msg.getElementAsString("ioi_big_qualifiers_0") : "";
					String ioi_big_qualifiers_1 = msg.hasElement("ioi_big_qualifiers_1") ? msg.getElementAsString("ioi_big_qualifiers_1") : "";
					String ioi_big_qualifiers_2 = msg.hasElement("ioi_big_qualifiers_2") ? msg.getElementAsString("ioi_big_qualifiers_2") : "";
					String ioi_big_qualifiers_3 = msg.hasElement("ioi_big_qualifiers_3") ? msg.getElementAsString("ioi_big_qualifiers_3") : "";
					String ioi_big_qualifiers_4 = msg.hasElement("ioi_big_qualifiers_4") ? msg.getElementAsString("ioi_big_qualifiers_4") : "";
					Integer ioi_big_qualifiers_count = msg.hasElement("ioi_big_qualifiers_count") ? msg.getElementAsInt32("ioi_big_qualifiers_count") : 0;
					String ioi_bid_referencePrice_currency = msg.hasElement("ioi_bid_referencePrice_currency") ? msg.getElementAsString("ioi_bid_referencePrice_currency") : "";
					Double ioi_bid_referencePrice_price = msg.hasElement("ioi_bid_referencePrice_price") ? msg.getElementAsFloat64("ioi_bid_referencePrice_price") : 0;
					String ioi_bid_size_quality = msg.hasElement("ioi_bid_size_quality") ? msg.getElementAsString("ioi_bid_size_quality") : "";
					Long ioi_bid_size_quantity = msg.hasElement("ioi_bid_size_quantity") ? msg.getElementAsInt64("ioi_bid_size_quantity") : 0;
					String ioi_bid_size_type = msg.hasElement("ioi_bid_size_type") ? msg.getElementAsString("ioi_bid_size_type") : "";
					Double ioi_bid_volitility = msg.hasElement("ioi_bid_volitility") ? msg.getElementAsFloat64("ioi_bid_volitility") : 0;
					Datetime ioi_goodUntil = msg.hasElement("ioi_goodUntil") ? msg.getElementAsDatetime("ioi_goodUntil") : null;
					Double ioi_instrument_option_legs_0_delta = msg.hasElement("ioi_instrument_option_legs_0_delta") ? msg.getElementAsFloat64("ioi_instrument_option_legs_0_delta") : 0;
					String ioi_instrument_option_legs_0_exchange = msg.hasElement("ioi_instrument_option_legs_0_exchange") ? msg.getElementAsString("ioi_instrument_option_legs_0_exchange") : "";
					Datetime ioi_instrument_option_legs_0_expiry = msg.hasElement("ioi_instrument_option_legs_0_expiry") ? msg.getElementAsDatetime("ioi_instrument_option_legs_0_expiry") : null;
					Integer ioi_instrument_option_legs_0_futureRefDate = msg.hasElement("ioi_instrument_option_legs_0_futureRefDate") ? msg.getElementAsInt32("ioi_instrument_option_legs_0_futureRefDate") : 0;
					Double ioi_instrument_option_legs_0_ratio = msg.hasElement("ioi_instrument_option_legs_0_ratio") ? msg.getElementAsFloat64("ioi_instrument_option_legs_0_ratio") : 0;
					Double ioi_instrument_option_legs_0_strike = msg.hasElement("ioi_instrument_option_legs_0_strike") ? msg.getElementAsFloat64("ioi_instrument_option_legs_0_strike") : 0;
					String ioi_instrument_option_legs_0_style = msg.hasElement("ioi_instrument_option_legs_0_style") ? msg.getElementAsString("ioi_instrument_option_legs_0_style") : "";
					String ioi_instrument_option_legs_0_type = msg.hasElement("ioi_instrument_option_legs_0_type") ? msg.getElementAsString("ioi_instrument_option_legs_0_type") : "";
					String ioi_instrument_option_legs_0_underlying_figi = msg.hasElement("ioi_instrument_option_legs_0_underlying_figi") ? msg.getElementAsString("ioi_instrument_option_legs_0_underlying_figi") : "";
					String ioi_instrument_option_legs_0_underlying_ticker = msg.hasElement("ioi_instrument_option_legs_0_underlying_ticker") ? msg.getElementAsString("ioi_instrument_option_legs_0_underlying_ticker") : "";
					String ioi_instrument_option_legs_0_underlying_type = msg.hasElement("ioi_instrument_option_legs_0_underlying_type") ? msg.getElementAsString("ioi_instrument_option_legs_0_underlying_type") : "";
					Double ioi_instrument_option_legs_1_delta = msg.hasElement("ioi_instrument_option_legs_1_delta") ? msg.getElementAsFloat64("ioi_instrument_option_legs_1_delta") : 0;
					String ioi_instrument_option_legs_1_exchange = msg.hasElement("ioi_instrument_option_legs_1_exchange") ? msg.getElementAsString("ioi_instrument_option_legs_1_exchange") : "";
					Datetime ioi_instrument_option_legs_1_expiry = msg.hasElement("ioi_instrument_option_legs_1_expiry") ? msg.getElementAsDatetime("ioi_instrument_option_legs_1_expiry") : null;
					Integer ioi_instrument_option_legs_1_futureRefDate = msg.hasElement("ioi_instrument_option_legs_1_futureRefDate") ? msg.getElementAsInt32("ioi_instrument_option_legs_1_futureRefDate") : 0;
					Double ioi_instrument_option_legs_1_ratio = msg.hasElement("ioi_instrument_option_legs_1_ratio") ? msg.getElementAsFloat64("ioi_instrument_option_legs_1_ratio") : 0;
					Double ioi_instrument_option_legs_1_strike = msg.hasElement("ioi_instrument_option_legs_1_strike") ? msg.getElementAsFloat64("ioi_instrument_option_legs_1_strike") : 0;
					String ioi_instrument_option_legs_1_style = msg.hasElement("ioi_instrument_option_legs_1_style") ? msg.getElementAsString("ioi_instrument_option_legs_1_style") : "";
					String ioi_instrument_option_legs_1_type = msg.hasElement("ioi_instrument_option_legs_1_type") ? msg.getElementAsString("ioi_instrument_option_legs_1_type") : "";
					String ioi_instrument_option_legs_1_underlying_figi = msg.hasElement("ioi_instrument_option_legs_1_underlying_figi") ? msg.getElementAsString("ioi_instrument_option_legs_1_underlying_figi") : "";
					String ioi_instrument_option_legs_1_underlying_ticker = msg.hasElement("ioi_instrument_option_legs_1_underlying_ticker") ? msg.getElementAsString("ioi_instrument_option_legs_1_underlying_ticker") : "";
					String ioi_instrument_option_legs_1_underlying_type = msg.hasElement("ioi_instrument_option_legs_1_underlying_type") ? msg.getElementAsString("ioi_instrument_option_legs_1_underlying_type") : "";
					Double ioi_instrument_option_legs_2_delta = msg.hasElement("ioi_instrument_option_legs_2_delta") ? msg.getElementAsFloat64("ioi_instrument_option_legs_2_delta") : 0;
					String ioi_instrument_option_legs_2_exchange = msg.hasElement("ioi_instrument_option_legs_2_exchange") ? msg.getElementAsString("ioi_instrument_option_legs_2_exchange") : "";
					Integer ioi_instrument_option_legs_2_expiry = msg.hasElement("ioi_instrument_option_legs_2_expiry") ? msg.getElementAsInt32("ioi_instrument_option_legs_2_expiry") : 0;
					Integer ioi_instrument_option_legs_2_futureRefDate = msg.hasElement("ioi_instrument_option_legs_2_futureRefDate") ? msg.getElementAsInt32("ioi_instrument_option_legs_2_futureRefDate") : 0;
					Double ioi_instrument_option_legs_2_ratio = msg.hasElement("ioi_instrument_option_legs_2_ratio") ? msg.getElementAsFloat64("ioi_instrument_option_legs_2_ratio") : 0;
					Double ioi_instrument_option_legs_2_strike = msg.hasElement("ioi_instrument_option_legs_2_strike") ? msg.getElementAsFloat64("ioi_instrument_option_legs_2_strike") : 0;
					String ioi_instrument_option_legs_2_style = msg.hasElement("ioi_instrument_option_legs_2_style") ? msg.getElementAsString("ioi_instrument_option_legs_2_style") : "";
					String ioi_instrument_option_legs_2_type = msg.hasElement("ioi_instrument_option_legs_2_type") ? msg.getElementAsString("ioi_instrument_option_legs_2_type") : "";
					String ioi_instrument_option_legs_2_underlying_figi = msg.hasElement("ioi_instrument_option_legs_2_underlying_figi") ? msg.getElementAsString("ioi_instrument_option_legs_2_underlying_figi") : "";
					String ioi_instrument_option_legs_2_underlying_ticker = msg.hasElement("ioi_instrument_option_legs_2_underlying_ticker") ? msg.getElementAsString("ioi_instrument_option_legs_2_underlying_ticker") : "";
					String ioi_instrument_option_legs_2_underlying_type = msg.hasElement("ioi_instrument_option_legs_2_underlying_type") ? msg.getElementAsString("ioi_instrument_option_legs_2_underlying_type") : "";
					Double ioi_instrument_option_legs_3_delta = msg.hasElement("ioi_instrument_option_legs_3_delta") ? msg.getElementAsFloat64("ioi_instrument_option_legs_3_delta") : 0;
					String ioi_instrument_option_legs_3_exchange = msg.hasElement("ioi_instrument_option_legs_3_exchange") ? msg.getElementAsString("ioi_instrument_option_legs_3_exchange") : "";
					Integer ioi_instrument_option_legs_3_expiry = msg.hasElement("ioi_instrument_option_legs_3_expiry") ? msg.getElementAsInt32("ioi_instrument_option_legs_3_expiry") : 0;
					Integer ioi_instrument_option_legs_3_futureRefDate = msg.hasElement("ioi_instrument_option_legs_3_futureRefDate") ? msg.getElementAsInt32("ioi_instrument_option_legs_3_futureRefDate") : 0;
					Double ioi_instrument_option_legs_3_ratio = msg.hasElement("ioi_instrument_option_legs_3_ratio") ? msg.getElementAsFloat64("ioi_instrument_option_legs_3_ratio") : 0;
					Double ioi_instrument_option_legs_3_strike = msg.hasElement("ioi_instrument_option_legs_3_strike") ? msg.getElementAsFloat64("ioi_instrument_option_legs_3_strike") : 0;
					String ioi_instrument_option_legs_3_style = msg.hasElement("ioi_instrument_option_legs_3_style") ? msg.getElementAsString("ioi_instrument_option_legs_3_style") : "";
					String ioi_instrument_option_legs_3_type = msg.hasElement("ioi_instrument_option_legs_3_type") ? msg.getElementAsString("ioi_instrument_option_legs_3_type") : "";
					String ioi_instrument_option_legs_3_underlying_figi = msg.hasElement("ioi_instrument_option_legs_3_underlying_figi") ? msg.getElementAsString("ioi_instrument_option_legs_3_underlying_figi") : "";
					String ioi_instrument_option_legs_3_underlying_ticker = msg.hasElement("ioi_instrument_option_legs_3_underlying_ticker") ? msg.getElementAsString("ioi_instrument_option_legs_3_underlying_ticker") : "";
					String ioi_instrument_option_legs_3_underlying_type = msg.hasElement("ioi_instrument_option_legs_3_underlying_type") ? msg.getElementAsString("ioi_instrument_option_legs_3_underlying_type") : "";
					Integer ioi_instrument_option_legs_count = msg.hasElement("ioi_instrument_option_legs_count") ? msg.getElementAsInt32("ioi_instrument_option_legs_count") : 0;
					String ioi_instrument_option_structure = msg.hasElement("ioi_instrument_option_structure") ? msg.getElementAsString("ioi_instrument_option_structure") : "";
					String ioi_instrument_stock_security_figi = msg.hasElement("ioi_instrument_stock_security_figi") ? msg.getElementAsString("ioi_instrument_stock_security_figi") : "";
					String ioi_instrument_stock_security_ticker = msg.hasElement("ioi_instrument_stock_security_ticker") ? msg.getElementAsString("ioi_instrument_stock_security_ticker") : "";
					String ioi_instrument_type = msg.hasElement("ioi_instrument_type") ? msg.getElementAsString("ioi_instrument_type") : "";
					String ioi_offer_notes = msg.hasElement("ioi_offer_notes") ? msg.getElementAsString("ioi_offer_notes") : "";
					String ioi_offer_price_fixed_currency = msg.hasElement("ioi_offer_price_fixed_currency") ? msg.getElementAsString("ioi_offer_price_fixed_currency") : "";
					Double ioi_offer_price_fixed_price = msg.hasElement("ioi_offer_price_fixed_price") ? msg.getElementAsFloat64("ioi_offer_price_fixed_price") : 0;
					Double ioi_offer_price_moneyness = msg.hasElement("ioi_offer_price_moneyness") ? msg.getElementAsFloat64("ioi_offer_price_moneyness") : 0;
					Double ioi_offer_price_pegged_limitPrice = msg.hasElement("ioi_offer_price_pegged_limitPrice") ? msg.getElementAsFloat64("ioi_offer_price_pegged_limitPrice") : 0;
					Double ioi_offer_price_pegged_offsetAmount = msg.hasElement("ioi_offer_price_pegged_offsetAmount") ? msg.getElementAsFloat64("ioi_offer_price_pegged_offsetAmount") : 0;
					String ioi_offer_price_pegged_offsetFrom = msg.hasElement("ioi_offer_price_pegged_offsetFrom") ? msg.getElementAsString("ioi_offer_price_pegged_offsetFrom") : "";
					String ioi_offer_price_reference = msg.hasElement("ioi_offer_price_reference") ? msg.getElementAsString("ioi_offer_price_reference") : "";
					String ioi_offer_price_type = msg.hasElement("ioi_offer_price_type") ? msg.getElementAsString("ioi_offer_price_type") : "";
					String ioi_offer_qualifiers_0 = msg.hasElement("ioi_offer_qualifiers_0") ? msg.getElementAsString("ioi_offer_qualifiers_0") : "";
					String ioi_offer_qualifiers_1 = msg.hasElement("ioi_offer_qualifiers_1") ? msg.getElementAsString("ioi_offer_qualifiers_1") : "";
					String ioi_offer_qualifiers_2 = msg.hasElement("ioi_offer_qualifiers_2") ? msg.getElementAsString("ioi_offer_qualifiers_2") : "";
					String ioi_offer_qualifiers_3 = msg.hasElement("ioi_offer_qualifiers_3") ? msg.getElementAsString("ioi_offer_qualifiers_3") : "";
					String ioi_offer_qualifiers_4 = msg.hasElement("ioi_offer_qualifiers_4") ? msg.getElementAsString("ioi_offer_qualifiers_4") : "";
					Integer ioi_offer_qualifiers_count = msg.hasElement("ioi_offer_qualifiers_count") ? msg.getElementAsInt32("ioi_offer_qualifiers_count") : 0;
					String ioi_offer_referencePrice_currency = msg.hasElement("ioi_offer_referencePrice_currency") ? msg.getElementAsString("ioi_offer_referencePrice_currency") : "";
					Double ioi_offer_referencePrice_price = msg.hasElement("ioi_offer_referencePrice_price") ? msg.getElementAsFloat64("ioi_offer_referencePrice_price") : 0;
					String ioi_offer_size_quality = msg.hasElement("ioi_offer_size_quality") ? msg.getElementAsString("ioi_offer_size_quality") : "";
					Long ioi_offer_size_quantity = msg.hasElement("ioi_offer_size_quantity") ? msg.getElementAsInt64("ioi_offer_size_quantity") : 0;
					String ioi_offer_size_type = msg.hasElement("ioi_offer_size_type") ? msg.getElementAsString("ioi_offer_size_type") : "";
					Double ioi_offer_volitility = msg.hasElement("ioi_offer_volitility") ? msg.getElementAsFloat64("ioi_offer_volitility") : 0;
					String ioi_routing_broker = msg.hasElement("ioi_routing_broker") ? msg.getElementAsString("ioi_routing_broker") : "";
					String ioi_routing_customId = msg.hasElement("ioi_routing_customId") ? msg.getElementAsString("ioi_routing_customId") : "";
					String ioi_routing_strategy_brief = msg.hasElement("ioi_routing_strategy_brief") ? msg.getElementAsString("ioi_routing_strategy_brief") : "";
					String ioi_routing_strategy_detailed = msg.hasElement("ioi_routing_strategy_detailed") ? msg.getElementAsString("ioi_routing_strategy_detailed") : "";
					String ioi_routing_strategy_name = msg.hasElement("ioi_routing_strategy_name") ? msg.getElementAsString("ioi_routing_strategy_name") : "";
					Datetime ioi_sentTime = msg.hasElement("ioi_sentTime") ? msg.getElementAsDatetime("ioi_sentTime") : null;
    					
					System.out.println("\nIOI MESSAGE: ");
                
					System.out.println("change: " + change);
					System.out.println("id_value: " + id_value);
					System.out.println("originalId_value: " + originalId_value);
					System.out.println("ioi_bid_notes: " + ioi_bid_notes);
					System.out.println("ioi_bid_price_fixed_currency: " + ioi_bid_price_fixed_currency);
					System.out.println("ioi_bid_price_fixed_price: " + ioi_bid_price_fixed_price);
					System.out.println("ioi_bid_price_moneyness: " + ioi_bid_price_moneyness);
					System.out.println("ioi_bid_price_pegged_limitPrice: " + ioi_bid_price_pegged_limitPrice);
					System.out.println("ioi_bid_price_pegged_offsetAmount: " + ioi_bid_price_pegged_offsetAmount);
					System.out.println("ioi_bid_price_pegged_offsetFrom: " + ioi_bid_price_pegged_offsetFrom);
					System.out.println("ioi_bid_price_reference: " + ioi_bid_price_reference);
					System.out.println("ioi_bid_price_type: " + ioi_bid_price_type);
					System.out.println("ioi_big_qualifiers_0: " + ioi_big_qualifiers_0);
					System.out.println("ioi_big_qualifiers_1: " + ioi_big_qualifiers_1);
					System.out.println("ioi_big_qualifiers_2: " + ioi_big_qualifiers_2);
					System.out.println("ioi_big_qualifiers_3: " + ioi_big_qualifiers_3);
					System.out.println("ioi_big_qualifiers_4: " + ioi_big_qualifiers_4);
					System.out.println("ioi_big_qualifiers_count: " + ioi_big_qualifiers_count);
					System.out.println("ioi_bid_referencePrice_currency: " + ioi_bid_referencePrice_currency);
					System.out.println("ioi_bid_referencePrice_price: " + ioi_bid_referencePrice_price);
					System.out.println("ioi_bid_size_quality: " + ioi_bid_size_quality);
					System.out.println("ioi_bid_size_quantity: " + ioi_bid_size_quantity);
					System.out.println("ioi_bid_size_type: " + ioi_bid_size_type);
					System.out.println("ioi_bid_volitility: " + ioi_bid_volitility);
					System.out.println("ioi_goodUntil: " + ioi_goodUntil);
					System.out.println("ioi_instrument_option_legs_0_delta: " + ioi_instrument_option_legs_0_delta);
					System.out.println("ioi_instrument_option_legs_0_exchange: " + ioi_instrument_option_legs_0_exchange);
					System.out.println("ioi_instrument_option_legs_0_expiry: " + ioi_instrument_option_legs_0_expiry);
					System.out.println("ioi_instrument_option_legs_0_futureRefDate: " + ioi_instrument_option_legs_0_futureRefDate);
					System.out.println("ioi_instrument_option_legs_0_ratio: " + ioi_instrument_option_legs_0_ratio);
					System.out.println("ioi_instrument_option_legs_0_strike: " + ioi_instrument_option_legs_0_strike);
					System.out.println("ioi_instrument_option_legs_0_style: " + ioi_instrument_option_legs_0_style);
					System.out.println("ioi_instrument_option_legs_0_type: " + ioi_instrument_option_legs_0_type);
					System.out.println("ioi_instrument_option_legs_0_underlying_figi: " + ioi_instrument_option_legs_0_underlying_figi);
					System.out.println("ioi_instrument_option_legs_0_underlying_ticker: " + ioi_instrument_option_legs_0_underlying_ticker);
					System.out.println("ioi_instrument_option_legs_0_underlying_type: " + ioi_instrument_option_legs_0_underlying_type);
					System.out.println("ioi_instrument_option_legs_1_delta: " + ioi_instrument_option_legs_1_delta);
					System.out.println("ioi_instrument_option_legs_1_exchange: " + ioi_instrument_option_legs_1_exchange);
					System.out.println("ioi_instrument_option_legs_1_expiry: " + ioi_instrument_option_legs_1_expiry);
					System.out.println("ioi_instrument_option_legs_1_futureRefDate: " + ioi_instrument_option_legs_1_futureRefDate);
					System.out.println("ioi_instrument_option_legs_1_ratio: " + ioi_instrument_option_legs_1_ratio);
					System.out.println("ioi_instrument_option_legs_1_strike: " + ioi_instrument_option_legs_1_strike);
					System.out.println("ioi_instrument_option_legs_1_style: " + ioi_instrument_option_legs_1_style);
					System.out.println("ioi_instrument_option_legs_1_type: " + ioi_instrument_option_legs_1_type);
					System.out.println("ioi_instrument_option_legs_1_underlying_figi: " + ioi_instrument_option_legs_1_underlying_figi);
					System.out.println("ioi_instrument_option_legs_1_underlying_ticker: " + ioi_instrument_option_legs_1_underlying_ticker);
					System.out.println("ioi_instrument_option_legs_1_underlying_type: " + ioi_instrument_option_legs_1_underlying_type);
					System.out.println("ioi_instrument_option_legs_2_delta: " + ioi_instrument_option_legs_2_delta);
					System.out.println("ioi_instrument_option_legs_2_exchange: " + ioi_instrument_option_legs_2_exchange);
					System.out.println("ioi_instrument_option_legs_2_expiry: " + ioi_instrument_option_legs_2_expiry);
					System.out.println("ioi_instrument_option_legs_2_futureRefDate: " + ioi_instrument_option_legs_2_futureRefDate);
					System.out.println("ioi_instrument_option_legs_2_ratio: " + ioi_instrument_option_legs_2_ratio);
					System.out.println("ioi_instrument_option_legs_2_strike: " + ioi_instrument_option_legs_2_strike);
					System.out.println("ioi_instrument_option_legs_2_style: " + ioi_instrument_option_legs_2_style);
					System.out.println("ioi_instrument_option_legs_2_type: " + ioi_instrument_option_legs_2_type);
					System.out.println("ioi_instrument_option_legs_2_underlying_figi: " + ioi_instrument_option_legs_2_underlying_figi);
					System.out.println("ioi_instrument_option_legs_2_underlying_ticker: " + ioi_instrument_option_legs_2_underlying_ticker);
					System.out.println("ioi_instrument_option_legs_2_underlying_type: " + ioi_instrument_option_legs_2_underlying_type);
					System.out.println("ioi_instrument_option_legs_3_delta: " + ioi_instrument_option_legs_3_delta);
					System.out.println("ioi_instrument_option_legs_3_exchange: " + ioi_instrument_option_legs_3_exchange);
					System.out.println("ioi_instrument_option_legs_3_expiry: " + ioi_instrument_option_legs_3_expiry);
					System.out.println("ioi_instrument_option_legs_3_futureRefDate: " + ioi_instrument_option_legs_3_futureRefDate);
					System.out.println("ioi_instrument_option_legs_3_ratio: " + ioi_instrument_option_legs_3_ratio);
					System.out.println("ioi_instrument_option_legs_3_strike: " + ioi_instrument_option_legs_3_strike);
					System.out.println("ioi_instrument_option_legs_3_style: " + ioi_instrument_option_legs_3_style);
					System.out.println("ioi_instrument_option_legs_3_type: " + ioi_instrument_option_legs_3_type);
					System.out.println("ioi_instrument_option_legs_3_underlying_figi: " + ioi_instrument_option_legs_3_underlying_figi);
					System.out.println("ioi_instrument_option_legs_3_underlying_ticker: " + ioi_instrument_option_legs_3_underlying_ticker);
					System.out.println("ioi_instrument_option_legs_3_underlying_type: " + ioi_instrument_option_legs_3_underlying_type);
					System.out.println("ioi_instrument_option_legs_count: " + ioi_instrument_option_legs_count);
					System.out.println("ioi_instrument_option_structure: " + ioi_instrument_option_structure);
					System.out.println("ioi_instrument_stock_security_figi: " + ioi_instrument_stock_security_figi);
					System.out.println("ioi_instrument_stock_security_ticker: " + ioi_instrument_stock_security_ticker);
					System.out.println("ioi_instrument_type: " + ioi_instrument_type);
					System.out.println("ioi_offer_notes: " + ioi_offer_notes);
					System.out.println("ioi_offer_price_fixed_currency: " + ioi_offer_price_fixed_currency);
					System.out.println("ioi_offer_price_fixed_price: " + ioi_offer_price_fixed_price);
					System.out.println("ioi_offer_price_moneyness: " + ioi_offer_price_moneyness);
					System.out.println("ioi_offer_price_pegged_limitPrice: " + ioi_offer_price_pegged_limitPrice);
					System.out.println("ioi_offer_price_pegged_offsetAmount: " + ioi_offer_price_pegged_offsetAmount);
					System.out.println("ioi_offer_price_pegged_offsetFrom: " + ioi_offer_price_pegged_offsetFrom);
					System.out.println("ioi_offer_price_reference: " + ioi_offer_price_reference);
					System.out.println("ioi_offer_price_type: " + ioi_offer_price_type);
					System.out.println("ioi_offer_qualifiers_0: " + ioi_offer_qualifiers_0);
					System.out.println("ioi_offer_qualifiers_1: " + ioi_offer_qualifiers_1);
					System.out.println("ioi_offer_qualifiers_2: " + ioi_offer_qualifiers_2);
					System.out.println("ioi_offer_qualifiers_3: " + ioi_offer_qualifiers_3);
					System.out.println("ioi_offer_qualifiers_4: " + ioi_offer_qualifiers_4);
					System.out.println("ioi_offer_qualifiers_count: " + ioi_offer_qualifiers_count);
					System.out.println("ioi_offer_referencePrice_currency: " + ioi_offer_referencePrice_currency);
					System.out.println("ioi_offer_referencePrice_price: " + ioi_offer_referencePrice_price);
					System.out.println("ioi_offer_size_quality: " + ioi_offer_size_quality);
					System.out.println("ioi_offer_size_quantity: " + ioi_offer_size_quantity);
					System.out.println("ioi_offer_size_type: " + ioi_offer_size_type);
					System.out.println("ioi_offer_volitility: " + ioi_offer_volitility);
					System.out.println("ioi_routing_broker: " + ioi_routing_broker);
					System.out.println("ioi_routing_customId: " + ioi_routing_customId);
					System.out.println("ioi_routing_strategy_brief: " + ioi_routing_strategy_brief);
					System.out.println("ioi_routing_strategy_detailed: " + ioi_routing_strategy_detailed);
					System.out.println("ioi_routing_strategy_name: " + ioi_routing_strategy_name);
					System.out.println("ioi_sentTime: " + (ioi_sentTime!=null ? ioi_sentTime.toString() : "null"));
        				
        		} else {
        			System.err.println("Error: Unexpected message");
        		}
                
            }
            return true;
        }

        private boolean processMiscEvents(Event event, Session session) throws Exception 
        {
            System.out.println("Processing " + event.eventType().toString());
            MessageIterator msgIter = event.messageIterator();
            while (msgIter.hasNext()) {
                Message msg = msgIter.next();
                System.out.println("MESSAGE: " + msg);
            }
            return true;
        }
        
        private void createSubscription(Session session) 
        {
        	System.out.println("Create IOI subscription");

        	String ioiTopic = d_service + "/ioi";
        	//String ioiTopic = d_service + "/ioi?uuid=8049857";
    		

        	ioiSubscription = new Subscription(ioiTopic);
        	System.out.println("Topic: " + ioiTopic);
        	SubscriptionList subscriptions = new SubscriptionList();
        	subscriptions.add(ioiSubscription);

        	try {
        		session.subscribe(subscriptions);
        	} catch (Exception ex) {
        		System.err.println("Failed to create subscription");
        	}
        	
        }

    }	
}


