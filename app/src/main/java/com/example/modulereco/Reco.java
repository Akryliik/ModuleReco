package com.example.modulereco;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class Reco extends Activity
{
	Exercice exo = null;
	Recorder rec = null;
	Alignement alignement = null;
	DAP dap = null;

	TextView mot = null;
	TextView compteur = null;
	Button enregistrer = null;

	ArrayList<String> tabMot = null;
	ArrayList<String> tabPhoneme = null;
	ArrayList<String> tabDap = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reco);

		mot = findViewById(R.id.mot);
		compteur = findViewById(R.id.compteur);
		enregistrer = findViewById(R.id.record);

		tabMot = new ArrayList<>();
		tabPhoneme = new ArrayList<>();
		tabDap = new ArrayList<>();

		dap = new DAP(this);
		exo = new Exercice(10, this);

		initialiser();
		creerDossier();

		enregistrer.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (!rec.getRecording())
				{
					rec.startRecording();
					enregistrer.setText("STOP");
				}
				else
				{
					rec.stopRecording();

					analyser();

					actualiser();
					enregistrer.setText("Enregistrer");
				}
			}
		});
	}

	private void actualiser()
	{
		exo.next();
		initialiser();
	}

	private void initialiser()
	{
		mot.setText("" + exo.getMot());
		compteur.setText((exo.getIndex() + 1) + "/" + exo.getMaxMots());

		rec = new Recorder("" + exo.getMot());
	}

	private void analyser()
	{
		clearTab();

		File wav = new File(rec.getFilename());

		alignement = new Alignement(Reco.this, Alignement.MOT);
		tabMot = alignement.convertir(wav);

		alignement = new Alignement(Reco.this, Alignement.PHONEME);
		tabPhoneme = alignement.convertir(wav);

		tabDap = dap.convertir(wav);
	}

	private void clearTab()
	{
		if (!tabMot.isEmpty())
			tabMot.clear();

		if (!tabPhoneme.isEmpty())
			tabPhoneme.clear();

		if (!tabDap.isEmpty())
			tabDap.clear();
	}

	private void creerDossier()
	{
		File file = new File(Environment.getExternalStorageDirectory().getPath(),"ModuleReco/Exercices/Exo" + rec.getCurrentTimeUsingCalendar("1"));

		if (!file.exists())
			file.mkdirs();

		rec.setExo("Exo" + rec.getCurrentTimeUsingCalendar("1"));
	}
}