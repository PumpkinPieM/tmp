/* DO NOT!!! put a package statement here, that would break the build system */
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

class Record{
		public int frames;
		public int bytes;
		public Record(int frames, int bytes){
			this.frames = frames;
			this.bytes = bytes;
		}
}
public class Assignment2 {

/*====================================TODO===================================*/
	/* Put your required state variables here */
	static byte[] mymac;
	static HashMap records;
	static int frameToMe;
	static int frameMulti;
	static int num_ipv4;
	static int num_ipv6;
	static int frameCounter;

/*===========================================================================*/

	public static void run(GRNVS_RAW sock, int frames) {
		byte[] recbuffer = new byte[1514];
		int length;
		Timeout timeout = new Timeout(10000);

/*====================================TODO===================================*/
	/* If you want to set up any data/counters before the receive loop,
	 * this is the right location
	 */
		mymac = sock.getMac();
		records = new HashMap();
		frameCounter = 0;

		StringBuilder sb = new StringBuilder();
		for (byte b : mymac){
			sb.append(String.format("%02X", b));
		}
		String mymac_str = sb.toString();


/*===========================================================================*/

	/* keep this AS IS! the tester uses this to avoid races */
		System.out.format("I am ready!\n");

/*====================================TODO===================================*/
	/* Update loop condition */
		for (int i=0;i<frames;i++ ) {
/*===========================================================================*/
			length = sock.read(recbuffer, timeout);
			if (length == 0) {
				System.err.format("Timed out, this means there was nothing to receive. Do you have a sender set up?\n");
				break;
			}
/*====================================TODO===================================*/
	/* This is the receive loop, 'recbuffer' will contain the received
	 * frame. 'length' tells you the length of what you received.
	 * Anything that should be done with every frame that's received
	 * should be done here.
	 */

/*===========================================================================*/
			frameCounter += 1;
			byte[] frame = Arrays.copyOfRange(recbuffer,0,length-1);
			sb = new StringBuilder();
			for (byte b : frame){
				sb.append(String.format("%02X", b));
			}
			String frame_str = sb.toString();
			String destMac = sb.substring(0,12);
			String ethertype = sb.substring(24,28);
			int multicast_flag = recbuffer[0]%2;
			int payload_len = (sb.length()-36)/2;

			if(records.containsKey(ethertype)){
				Record oldRecord = (Record)records.get(ethertype);
				oldRecord.frames += 1;
				oldRecord.bytes += payload_len;
				records.put(ethertype,oldRecord);
			}else{
				Record record = new Record(1,payload_len);
				records.put(ethertype, record);
			}


			if(multicast_flag == 1){ frameMulti += 1; }

			if (destMac.equals(mymac_str)){
				frameToMe += 1;
			}

			if(ethertype.equals("0800")) {
				num_ipv4 += 1;
			}else if(ethertype.equals("86DD")){
				num_ipv6 += 1;
			}


		}
/*====================================TODO===================================*/
	/* Print your summary here */
		Set set = records.entrySet();
		Iterator iter = set.iterator();
		while(iter.hasNext()){
			Map.Entry record = (Map.Entry) iter.next();
			Record r = (Record) record.getValue();
			System.out.printf("0x%s: %d frames, %d bytes\n",record.getKey(),r.frames,r.bytes);
		}
		System.out.printf("%d of them were for me\n",frameToMe);
		System.out.printf("%d of them were multicast\n",frameMulti);
		System.out.printf("IPv4 accounted for %.1f%% and IPv6 for %.1f%% of traffic", (float)num_ipv4/(float)frameCounter, (float)num_ipv6/(float)frameCounter);

/*===========================================================================*/
	}


	public static void main(String[] argv) {
		Arguments args = new Arguments(argv);
		GRNVS_RAW sock = null;
		try{
			sock = new GRNVS_RAW(args.iface, 3);
			run(sock, args.frames);
			sock.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.exit(1);
		}
	}
}
