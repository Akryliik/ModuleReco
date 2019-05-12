package com.example.modulereco;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Reco extends Activity
{
	Exercice exo = null;

	Recorder rec = null;
	Alignement alignement = null;
	DAP dap = null;
	AlignementSemi alignementSemi = null;
	TextView mot = null;
	TextView compteur = null;
	Button enregistrer = null;

	Button btEnd = null;
	Button retour = null;
	int type = 0;  // 1 = exo avec mot 0 = exo avec phrase
	int nbtest = 0;

	ArrayList<String> tabPhrase = null;
	ArrayList<String> tabPhoneme = null;
	ArrayList<String> tabDap = null;
	ArrayList<String> tabSemi = null;


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reco);
		findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);
		verifierPermissions();

		mot = findViewById(R.id.mot);
		compteur = findViewById(R.id.compteur);
		enregistrer = findViewById(R.id.record);
		btEnd = findViewById(R.id.btnEnd);
		retour = findViewById(R.id.back);

		Intent intent = getIntent();
		type = intent.getIntExtra("type", 1);
		nbtest = intent.getIntExtra("nbtest", 3);

		if (type == 1)
		{
			tabPhoneme = new ArrayList<>();
			tabDap = new ArrayList<>();
			tabSemi = new ArrayList<>();
			alignementSemi = new AlignementSemi(this);
			dap = new DAP(this);
			exo = new ExerciceMot(nbtest, this);
		}
		else if (type == 2)
		{
			tabPhrase = new ArrayList<>();
			exo = new ExerciceSeguin(nbtest, this);
		}

		initialiser();
		creerDossier();

		enregistrer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!rec.getRecording())
				{
					verifierPermissions();
					rec.startRecording();
					retour.setEnabled(false);
					enregistrer.setText("STOP");
				}
				else
				{
					rec.stopRecording();
                    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
					analyser();
					try
					{
						sauverResultats();
					}
					catch (IOException e)
					{
						e.printStackTrace();
					}
					actualiser();
					enregistrer.setText("Enregistrer");

					if (exo.getIndex() > 0)
						retour.setEnabled(true);
                    findViewById(R.id.loadingPanel).setVisibility(View.INVISIBLE);
				}
			}
		});

		retour.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				exo.prev();
				initialiser();
				btEnd.setEnabled(false);

				if (exo.getIndex() == 0)
					retour.setEnabled(false);
			}
		});

	}

	private void actualiser()
	{
	    if(!exo.fini())
        {
            exo.next();
            initialiser();
        }
		else
        {
            final Intent intent = new Intent(this, choixResultat.class);

            mot.setText("Exercice terminé !");
            compteur.setText("");

            btEnd.setVisibility(View.VISIBLE);
            btEnd.setOnClickListener(new Button.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startActivity(intent);
                }
            });
        }
	}

	private void verifierPermissions()
	{
		if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
				(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) &&
				(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED))
		{
			requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO }, 1);
		}
	}

	private void initialiser()
	{
		mot.setText(exo.getText());
		compteur.setText(exo.getIndex() + "/" + exo.getMax());
		if (type == 1)
			rec = new Recorder("" + exo.getText());
		else
			rec = new Recorder("" + exo.getIndex());
	}

	private void analyser()
	{
		clearTab();

		File wav = new File(rec.getFilename());

		if (type == 1)
		{
			alignement = new Alignement(Reco.this, Alignement.PHONEME);
			tabPhoneme = alignement.convertir(wav);
			alignementSemi = new AlignementSemi(Reco.this);
			tabSemi = alignementSemi.convertir(wav, tabPhoneme); // On renvoie les resultat de l'alignement pour récup les frontières
			dap = new DAP(Reco.this);
			tabDap = dap.convertir(wav);
		}
		else if (type == 2)
		{
			alignement = new Alignement(Reco.this, Alignement.VOISIN);
			tabPhrase = alignement.convertir(wav);
		}
	}

	private void sauverResultats() throws IOException
	{
		String nom = rec.getFilename();
		nom = nom.substring(0, nom.length() - 4);

		if (type == 1)
		{
			FileWriter writer = new FileWriter(nom + "-score-phoneme.txt");

			for (String str : tabPhoneme)
				writer.write(str + "\n");

			writer.close();

			writer = new FileWriter(nom + "-score-semi.txt");

			for (String str : tabSemi)
				writer.write(str + "\n");

			writer.close();

			writer = new FileWriter(nom + "-score-dap.txt");

			for (String str : tabDap)
				writer.write(str + "\n");

			writer.close();
		}
		else if (type == 2)
		{
			FileWriter writer = new FileWriter(nom + "-score.txt");

			for (String str : tabPhrase)
				writer.write(str + "\n");

			writer.close();
		}
	}

	private void clearTab()
	{
		if (type == 1)
		{
			if(tabPhoneme != null)
				if (!tabPhoneme.isEmpty())
					tabPhoneme.clear();
			if(tabDap != null)
				if (!tabDap.isEmpty())
					tabDap.clear();
		}
		else if (type == 2)
		{
			if(tabPhrase != null)
				if (!tabPhrase.isEmpty())
					tabPhrase.clear();
		}
	}

	private void creerDossier()
	{
		File file = new File(Environment.getExternalStorageDirectory().getPath(),"ModuleReco/Exercices/Exo" + rec.getCurrentTimeUsingCalendar("1"));

		if (!file.exists())
			file.mkdirs();

		rec.setExo("Exo" + rec.getCurrentTimeUsingCalendar("1"));
	}
}
