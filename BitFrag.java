package net.comploud.code.bitfrag;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.UUID;

/**
 * Chops a stream or block of bytes into fragments.
 * Created by tek-ti on 2014-09-08.
 */
public class BitFrag {
    public static void main(String argv[]) {
        // Here be spagetti code for now

        if(argv.length < 1) {
            System.out.println("BitFrag v0.1");
            System.out.println("Usage: BitFrag [-d] <file(s)>");
            System.out.println();
            System.out.println("\t-d\tDefrag files");
            System.exit(1);
        } else if(argv.length > 2 || argv[0].equals("-d")) {
            // Do defragmentation of input files
            System.out.println("BitFrag v0.1 - Defragger mode");
            // Open and read all input files
            HashSet<Fragment> frags = new HashSet<Fragment>();
            for(int i = 1; i < argv.length; i++) {
                try {
                    File infile = new File(argv[i]);
                    System.out.print("Reading fragment file: " + infile + ": ");
                    System.out.flush();
                    Fragment frag = Fragment.parseFragment(new FileInputStream(infile));
                    frags.add(frag);
                    System.out.println("OK");
                } catch(IOException e) {
                    System.out.println("Failed (IO): " + e.getMessage());
                } catch(FragmentFormatException e) {
                    System.out.println("Failed: " + e.getMessage());
                }
            }

            // Now try to match these fragments to each other in order to build a cluster (if possible)
            // TODO Put this in some helper class?
            HashSet<Cluster> clusters = new HashSet<Cluster>();
            for(Fragment frag : frags) {    // TODO THIS CODE MISBEHAVES!
                if(clusters.add(frag.getCluster())) {
                    // This is a newly discovered cluster!
                    System.out.println("Discovered cluster: " + frag.getCluster().getUuid());
                } else {
                    // Since this fragments' cluster is already known, merge into it!
                    for(Cluster clust : clusters) {
                        // A little bit of a hassle, but find the known cluster this way...
                        if(frag.getCluster().equals(clust)) {
                            try {
                                frag.mergeInto(clust);
                                //System.out.println("Merged fragment " + frag.getFragUuid() + " into cluster " + clust.getUuid());
                            } catch(InvalidClusterException e) {
                                // The premises to this exception are covered and hence will not occur
                            }
                        }
                    }
                }
            }

            // Attempt to defrag all discovered clusters (if possible)
            for(Cluster clust : clusters) {
                /*for(Iterator<Fragment> it = clust.iterator(); it.hasNext();) {
                    System.out.println("Cluster " + clust.getUuid() + " contains " + it.next().getFragUuid());
                }*/
                System.out.print("Reconstructing data for cluster " + clust.getUuid() + ": ");
                System.out.flush();
                byte[] data = clust.reconstructData();
                if(data == null) {
                    // Failed
                    System.out.println("Failed!");
                } else {
                    // Success
                    System.out.println("Success!");
                    File outfile = new File(clust.getUuid() + ".cluster");  // Keep it this simple for now
                    System.out.print("Writing data to file: " + outfile + ": ");
                    System.out.flush();
                    try {
                        FileOutputStream outstream = new FileOutputStream(outfile);
                        outstream.write(data);
                        System.out.println("OK");
                    } catch(IOException e) {
                        System.out.println("Failed (IO): " + e.getMessage());
                    }
                }
            }
        } else {
            // Do fragmentation of input file
            System.out.println("BitFrag v0.1 - Fragger mode");
            try {
                // Open and read the input file
                FileChannel infile = FileChannel.open(new File(argv[0]).toPath(), StandardOpenOption.READ);
                ByteBuffer data = ByteBuffer.allocate((int)infile.size());    // Hmm.. Ugly typecast.
                infile.read(data);
                Cluster cluster = new Cluster(data, 3, 1, (short)1);   // The parameters are "ignored" as of now

                System.out.println("Created cluster " + cluster.getUuid());
                System.out.println("Writing fragments to files:");
                for(Fragment frag : cluster) {
                    // Write this fragment to the file system (limited to current directory for now)
                    File outfile = new File(frag.getFragUuid() + ".frag");
                    System.out.print(outfile + ": ");
                    System.out.flush();
                    FileOutputStream outsream = new FileOutputStream(outfile);
                    frag.export(outsream);
                    System.out.println("OK");
                }
            } catch(IOException e) {
                System.out.println("Failed (IO): " + e.getMessage());
                System.exit(2);
            }
        }
    }
}
