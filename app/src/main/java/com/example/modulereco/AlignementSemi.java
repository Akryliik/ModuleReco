package com.example.modulereco;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Config;
import edu.cmu.pocketsphinx.Decoder;
import edu.cmu.pocketsphinx.Segment;

public class AlignementSemi {

    static { System.loadLibrary("pocketsphinx_jni"); }

    private InputStream streamFichier = null;
    private ArrayList<String> resultat;
    private Context contexte;
    private Decoder decoder = null;

    public AlignementSemi(Context contexte)
    {
        this.contexte = contexte;
        resultat = new ArrayList<>();

        try
        {
            Assets assets = new Assets(this.contexte);
            File assetsDir = assets.syncAssets();

            Config c = Decoder.defaultConfig();
            c.setString("-hmm", new File(assetsDir, "ptm").getPath());
            c.setString("-allphone", new File(assetsDir, "fr-phone.lm.dmp").getPath());
            c.setBoolean("-backtrace", true);
            c.setFloat("-beam", 1e-20);
            c.setFloat("-pbeam", 1e-20);
            c.setFloat("-lw", 2.0);

            decoder = new Decoder(c);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public ArrayList<String> convertir(final File fichier, ArrayList<String> timer)
    {
        try
        {
            streamFichier = new FileInputStream(fichier);
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e.getMessage());
            return null;
        }

        faireSemi(streamFichier, getTiming(timer));

        return resultat;
    }

    /**
     * Renvoie les timing de fin de frame afin de stoper le buffer à chaque frontière
     * @param timer Résultat obtenue par l'alignement
     * @return Timing de fin du phonème
     */
    private ArrayList<String> getTiming(ArrayList<String> timer)
    {
        ArrayList<String> res = new ArrayList<>();
        int index = 0;
        String [] array;
        for(String str : timer)
        {
            if(index > 0)
            {
                if(str.contains(" ")){
                    array = str.split("-");
                    String r = array[array.length-1];
                    str = r.substring(0, r.length()-1);
                }
                if(str.matches("[0-9]+") && str.length() > 2)
                    res.add(str);
            }
            index ++;
        }
        return res;
    }

    private void faireSemi(final InputStream stream, ArrayList<String> timer)
    {
        decoder.startUtt();
        byte[] b = new byte[4096];
        for(String r : timer)
            System.out.print(", "+r);
        try
        {
            int nbytes;
            for(String time : timer)
            {
                System.out.println("time -> "+time);
                while ((nbytes = stream.read(b,0, Integer.valueOf(time))) >= 0)
                {
                    ByteBuffer bb = ByteBuffer.wrap(b, 0, nbytes);

                    bb.order(ByteOrder.LITTLE_ENDIAN);

                    short[] s = new short[nbytes / 2];
                    bb.asShortBuffer().get(s);
                    decoder.processRaw(s, nbytes / 2, false, false);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println("Error when reading inputstream" + e.getMessage());
        }

        decoder.endUtt();

        int score = 0;
        int trames = 0;

        for (Segment seg : decoder.seg())
        {
            if (!seg.getWord().equals("SIL"))
            {
                trames += seg.getEndFrame() - seg.getStartFrame();
                score += seg.getAscore();
                //System.out.println("LSCORE = " + seg.getLscore());
                //System.out.println("PROB = " + seg.getProb());
            }
        }

        resultat.add("Score normalisé : " + ((float)score / trames) + "\n");

        for (Segment seg : decoder.seg())
        {
            int start = seg.getStartFrame(),
                    end   = seg.getEndFrame();
            String mot = seg.getWord();
            //System.out.println("Resultat -> "+start + " - " + end + " : " + mot + " (" + seg.getAscore() + ")");
            resultat.add(start + " - " + end + " : " + mot + " (" + seg.getAscore() + ")");
        }
    }
}
