package jrapl;

import java.lang.reflect.Field;

public class EnergyCheckUtils {
	public native static int scale(int freq);                   // not used yet in this file
	public native static int[] freqAvailable();                 // not used yet in this file
	public native static double[] GetPackagePowerSpec();        // not used yet in this file
	public native static double[] GetDramPowerSpec();           // not used yet in this file           
	public native static void SetPackagePowerLimit(int socketId, int level, double costomPower);        // not used yet in this file
    public native static void SetPackageTimeWindowLimit(int socketId, int level, double costomTimeWin); // not used yet in this file
    public native static void SetDramTimeWindowLimit(int socketId, int level, double costomTimeWin);    // not used yet in this file
    public native static void SetDramPowerLimit(int socketId, int level, double costomPower);           // not used yet in this file

	/** Documentation not done. See CPUScaler.c for source
	 *  Initializes the energy profile of the system. Starts collecting info
	 *  Finds the CPU model (stored in C var)
	 *  Finds number of suckets CPU has (stored in C var)
	 *  fills the 'fd' int array which holds some sort of information about the msr for a given core number
	 *
	 *  @return wraparound_energy a double representing the amount of energy in a socket or something
	*/
	public native static int ProfileInit();

	/** Documentation not done(?) See CPUScaler.c for source
	 * @return Number of CPU sockets the computer has
	*/
	public native static int GetSocketNum();

    /** Returns a string with energy information. 
        Formatted "1stSocketInfo @ 2ndSocketInfo @ ... @ NthSocketInfo" with @ delimiters
        Each NthSocketInfo subsection formatted "dram/uncore_energy#cpu_energy#package_energy" with # delimieters
        This string gets parsed into an array in getEnergyStats().
    */
    public native static String EnergyStatCheck();

	/** Frees memory allocated by ProfileInit() method.
	*/
	public native static void ProfileDealloc();

	public native static void SetPowerLimit(int ENABLE);       // not used yet in this file

	/**  Represents the energy in the rapl unit in a way that prevents bit overflow that would cause negative values. */
	public static int wraparoundValue;
	/** Number of sockets CPU has. Determined in ProfileInit() method. */
	public static int socketNum;

    /// the static block loads the library of native C calls from the JAR. also initializes a profile and gets number of CPU sockets	
    static {
		try {
			Field fieldSysPath = ClassLoader.class
					.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (Exception e) {

		}

		try {
		NativeUtils.loadLibraryFromJar("/jrapl/libCPUScaler.so");
		} catch (Exception e) {
			e.printStackTrace();
		}
		wraparoundValue = ProfileInit();
		socketNum = GetSocketNum();
	}

	/**
     * Parses string generated from the native EnergyStatCheck() method into an array of doubles.
	 * @return an array of current energy information.
     * Array will be size (3 * socketnum). There will be three entries per socket
	 * The first entry is: Dram/uncore gpu energy (depends on the cpu architecture)
	 * The second entry is: CPU energy
	 * The third entry is: Package energy
	 */

	public static double[] getEnergyStats() {
		socketNum = GetSocketNum();
		String EnergyInfo = EnergyStatCheck();
		/*One Socket*/
		if(socketNum == 1) {
			double[] stats = new double[3]; // 3 stats per socket
			String[] energy = EnergyInfo.split("#");

			stats[0] = Double.parseDouble(energy[0]);
			stats[1] = Double.parseDouble(energy[1]);
			stats[2] = Double.parseDouble(energy[2]);

			return stats;

		} else {
		/*Multiple sockets*/
			String[] perSockEner = EnergyInfo.split("@");
			double[] stats = new double[3*socketNum]; // 3 stats per socket
			int count = 0;


			for(int i = 0; i < perSockEner.length; i++) {
				String[] energy = perSockEner[i].split("#");
				for(int j = 0; j < energy.length; j++) {
					count = i * 3 + j;	//accumulative count
					stats[count] = Double.parseDouble(energy[j]);
				}
			}
			return stats;
		}
	}

  /** Frees memory allocated by profile initialization. Done at the end of the program.
  */
  public static void DeallocProfile() {
		ProfileDealloc();
  }

	public static void main(String[] args) {
		while(true) {
			double[] before = getEnergyStats();
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				System.err.format("Caught: " + e);
			}
			double[] after = getEnergyStats();
			System.out.println("dram: " + (after[0] - before[0])  + " cpu: " + (after[1] - before[1])  + " package: " + (after[2] - before[2])  );
		}
		// Unreachable
		//ProfileDealloc();
	}
}