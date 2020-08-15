package newicktree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//import java.util.function.Predicate;

public class ABipartition {
	
	/*
	 * Minimal number of donor sequences in in-group
	 * (for nested position, 1 is sufficient for a monophyly.
	 * for sole monophyly, this number might need to be increased.
	 * this parameter is not implemented for now)
	 */
	private final int MINIMAL_DONOR_NB = 1;
	
	/* different status and their coding */
	private final int MONOPHYLY     =  1;
	private final int NONMONOPHYLY  =  0;
	private final int ADJUSTABLE    = -1;
	private final int NODONOR       = -3;
	// set to 0, if query is not found in the tree (e.g., due to short length/large divergence)
	private int OTHERSCENARIO = 0;

	/* string variables */
	private String bipartition = new String();
	private String ingroup = new String();
	private String outgroup = new String();

	/* number of different donors */
	private int nb_donor;
	//private int nb_optional;
	
	/* Lists holding donor sequences, optional sequences and irrelevant sequences */
	private ArrayList<String> irrelevant= new ArrayList<String>();
	private ArrayList<String> donorSeqs = new ArrayList<String>();
	private ArrayList<String> optionSeqs= new ArrayList<String>();
	
	/** 
	 * Constructor 
	 * Input is two partitions joined by "\t". 
	 * In each partition, sequences are separated by ",".
	 * @param bipartition a newick tree string
	 */
	public ABipartition(String bipartition) { 
		this.bipartition = bipartition;
	}
	
	/**
	 * Return status of bipartition
	 * @return integer coding: 1 (monophyly), 0 (nonmonophyly), -1 (adjustable nonmonophyly), -3 (no donor found)
	 */
	public int getStatus(){
		if (OTHERSCENARIO != 0) {
			return OTHERSCENARIO;
		}else if ( nb_donor >= MINIMAL_DONOR_NB) {
			if (irrelevant.isEmpty()) {
				/* If no irrelevant taxon presents, monophyly is assumed */
				return MONOPHYLY;
			}else if (irrelevant.size()<3){
				/* If <3 sequences from irrelevant taxa presents, this bipartition is adjustable 
				 * This is a special type of non-monophyly. The irrelevant sequences might be due 
				 * to rare contamination 	
				 */				
				return ADJUSTABLE;			
			}else {
				/* If no irrelevant taxon presents, monophyly is assumed */
				return NONMONOPHYLY;
			}
		}else{
			/* if no donor sequence in in-group partition */
			return NODONOR; 
		}
	}
	
	/**
	 * Get the outgroup status
	 * @param cutoff a integer as a cutoff for the minimal number of sequences included in outgroup 
	 * @return status returns '0' when outgroup is valid and '-100' if otherwise
	 */
	public int getOutgroupStatus(int cutoff){
		int status = -100;
		if (outgroup.isEmpty()) return status;

		String[] leaves = outgroup.split(",");
		if (leaves.length >= cutoff) status = 0;		
		return status;
	}

	/** return a list holding donor sequences */
	public List<String> getDonorSeqs(){
		return donorSeqs;
	}
	
	/** return a list holding optional sequences */
	public List<String> getOptionalSeqs(){
		return optionSeqs;
	}
	
	/** return a lists holding irrelevant sequences if adjustable nonmonophyly*/
	public List<String> getMinorContamination(){
		return irrelevant;
	}
	
	/**
	 * Examine all in-group sequences with respect to donor, optional and irrelevant taxa
	 * update class variables: donorSeqs, optionSeqs, and irrelevant 
	 * @param query     Name of query taxa
	 * @param donor     Name(s) of donor taxa. Multiple taxa is joined with comma ","
	 * @param optionals Name(s) of optional taxa. Multiple taxa is joined with comma ","
	 * @param ingored   Name(s) of taxa that to be ignored. Multiple taxa is joined with comma ","
	 */
	public void checkIngroup(String query, String donor, String optionals, String ignored){
		//System.out.println("#ABipartition l62 checkIngroup: " + optionals + "\t" + ignored);
		nb_donor = 0;
		//nb_optional = 0;
		setInAndOutGroup(query);
		//System.out.println("#0");
		if (ingroup.isEmpty()) {
			OTHERSCENARIO = -2;
		}else {
			String[] leaves = ingroup.split(", ");
			//irrelevant= new ArrayList<String>();
			for (String l : leaves) {
				if (l.equals(query)){
					continue;
				//}else if(l.contains(donor)){
				}else if ( Arrays.asList(donor.split(",")).stream().anyMatch(l::contains) ){
					nb_donor +=1;
					donorSeqs.add(l);
					//System.out.println("#1 " + l + "\t" + nb_donor);
					//System.out.println("#2 " + l + "\t" + ingroup);
				}else{
					//if (!optionals.isEmpty() && Arrays.asList(optionals.split(",")).stream().anyMatch(isOptional(l))) {
					if (optionals.length()>0 && Arrays.asList(optionals.split(",")).stream().anyMatch(l::contains)) {
						optionSeqs.add(l);
					} else if (ignored.length()>0 && Arrays.asList(ignored.split(",")).stream().anyMatch(l::contains) ) {
							// do nothing
					} else {
							//System.out.println("#irrelevant " + l);
							irrelevant.add(l);
					}
				}
			}
		}
	}
	
	public void checkIngroup(String query, String donor, String optionals){
		String ignored = new String();
		checkIngroup(query, donor, optionals, ignored);
	}
	
	public void checkIngroup(String query, String donor){
		String optionals = new String();
		String ignored = new String();
		checkIngroup(query, donor, optionals, ignored);
	}

	/** figure out which one of the two partitions contains query (ie, in-group) */
	private void setInAndOutGroup(String query) {
		String[] partitions = bipartition.split("\t");
		if (partitions[0].contains(query)) {
			this.ingroup = partitions[0];
			this.outgroup = partitions[1];
		} else if (partitions[1].contains(query)) {
			this.ingroup = partitions[1];
			this.outgroup = partitions[0];
		}else {  ///???
			this.ingroup = "";
		}
	}

	
	public static void main(String[] args) {
		
		ABipartition bp = new ABipartition("Excavata.Parabasalia-Trichomonas_vaginalis.XP_001312029.1, Chromalveolata.Cryptophyta-Hemiselmis_viresens.PCC157.10436_1, Bacteria.Cyanobacteria-Nostoc_punctiforme.WP_012406913.1, Chromalveolata.Stramenopiles-Extubocellulus_spinifer.CCMP396.212444_1, Chromalveolata.Alveolata-Symbiodinium_kawagutii.Skav209349, Opisthokonta.Fungi-Grosmannia_clavigera.XP_014174961.1, Chromalveolata.Alveolata-Symbiodinium_kawagutii.Skav205108, Chromalveolata.Stramenopiles-Phytophthora_parasitica.XP_008909378.1, Opisthokonta.Metazoa-Lingula_anatina.XP_013388868.1, Bacteria.Firmicutes-Desulfotomaculum_ferrireducens.WP_077715261.1, Bacteria.Cyanobacteria-Fischerella_sp..WP_026733338.1, Opisthokonta.Fungi-Colletotrichum_gloeosporioides.XP_007286093.1, Chromalveolata.Cryptophyta-Rhodomonas_salina.CCMP1319.30247_1, Opisthokonta.Metazoa-Halyomorpha_halys.XP_014275896.1, Bacteria.DeinococcusThermus-Thermus_brockianus.WP_084720345.1, Opisthokonta.Fungi-Fonsecaea_erecta.XP_018688094.1, Chromalveolata.Haptophyceae-Exanthemachrysis_gayraliae.RCC1523.47428_1, Opisthokonta.Fungi-Pochonia_chlamydosporia.XP_018137502.1, Amoebozoa.Mycetozoa-Polysphondylium_pallidum.XP_020433757.1, Opisthokonta.Fungi-Trichoderma_virens.XP_013953319.1, Amoebozoa.Discosea-Acanthamoeba_castellanii.XP_004334895.1, Bacteria.Gammaproteobacteria-Thioalkalivibrio_sp..WP_081759346.1, Opisthokonta.Metazoa-Hydra_vulgaris.XP_012566385.1, Opisthokonta.Metazoa-Branchiostoma_belcheri.XP_019628839.1, Opisthokonta.Metazoa-Branchiostoma_belcheri.XP_019628842.1, Opisthokonta.Metazoa-Aedes_aegypti.XP_001649474.1, Chromalveolata.Stramenopiles-Saprolegnia_parasitica.XP_012210220.1, Excavata.Parabasalia-Trichomonas_vaginalis.XP_001316882.1, Plantae.Viridiplantae-Dunaliella_tertiolecta.CCMP1320.5362_1, Chromalveolata.Stramenopiles-Aphanomyces_invadans.XP_008878067.1, Opisthokonta.Fungi-Fonsecaea_erecta.XP_018688699.1, Chromalveolata.Stramenopiles-Pteridomonas_danica.PT_16066_1, Bacteria.Acidobacteria-Thermoanaerobaculum_aquaticum.WP_053335183.1, Opisthokonta.Metazoa-Aedes_aegypti.XP_001649301.1, Amoebozoa.Discosea-Acanthamoeba_castellanii.XP_004336589.1, Excavata.Parabasalia-Trichomonas_vaginalis.XP_001319033.1, Chromalveolata.Stramenopiles-Aureoumbra_lagunensis.CCMP1510.72868_1, Bacteria.Alphaproteobacteria-Wolbachia_endosymbiont.WP_015588929.1, Bacteria.Firmicutes-Orenia_marismortui.WP_018249643.1, Plantae.Rhodophyta-Porphyra_umbilicalis.OSX77492.1_17, Opisthokonta.Fungi-Pochonia_chlamydosporia.XP_018136334.1, Bacteria.Gammaproteobacteria-Candidatus_Berkiella.WP_075065856.1, Opisthokonta.Metazoa-Lingula_anatina.XP_013393107.1, Opisthokonta.Fungi-Penicillium_rubens.XP_002565858.1, Excavata.Parabasalia-Trichomonas_vaginalis.XP_001306731.1, Opisthokonta.Metazoa-Rhagoletis_zephyria.XP_017473340.1, Opisthokonta.Metazoa-Halyomorpha_halys.XP_014289686.1, Bacteria.Alphaproteobacteria-Candidatus_Jidaibacter.WP_053332443.1, Amoebozoa.Discosea-Acanthamoeba_castellanii.XP_004339446.1, Chromalveolata.Rhizaria-Paulinella_chromatophora.scaffold10087.g.72764, Chromalveolata.Haptophyceae-Isochrysis_galbana.CCMP1323.122318_1, Plantae.Rhodophyta-Porphyra_umbilicalis.OSX80214.1_35, Bacteria.Cyanobacteria-Mastigocladopsis_repens.WP_017316056.1, Bacteria.Cyanobacteria-Nostoc_sp..WP_069074761.1, Plantae.Rhodophyta-Porphyridium_aerugineum.MMETSP0313.14794_1, Chromalveolata.Cryptophyta-Hemiselmis_andersenii.CCMP1180.17211_1, Chromalveolata.Alveolata-Symbiodinium_minutum.v1.2.036614, Chromalveolata.Alveolata-Chromera_velia.Cvel_17240, Chromalveolata.Stramenopiles-Phytophthora_sojae.XP_009536741.1, Bacteria.Gammaproteobacteria-Thioalkalivibrio_sp..WP_025771962.1, Bacteria.Aquificae-Persephonella_sp..WP_051654700.1, Amoebozoa.Mycetozoa-Dictyostelium_fasciculatum.XP_004366622.1, Chromalveolata.Alveolata-Symbiodinium_minutum.v1.2.032370, Bacteria.Gammaproteobacteria-Diplorickettsia_massiliensis.WP_010597563.1, Chromalveolata.Stramenopiles-Phytophthora_ramorum.Phyra83968, Chromalveolata.Cryptophyta-Hemiselmis_viresens.PCC157.81498_1, Bacteria.Firmicutes-Desulfotomaculum_aeronauticum.WP_084082491.1, Amoebozoa.Mycetozoa-Dictyostelium_fasciculatum.XP_004350176.1, Chromalveolata.Stramenopiles-Pteridomonas_danica.PT_20646_1, Bacteria.Aquificae-Persephonella_marina.WP_049756059.1, Plantae.Viridiplantae-Dunaliella_tertiolecta.CCMP1320.123056_1, add.evm.model.contig44.4xx, Chromalveolata.Cryptophyta-Hemiselmis_andersenii.CCMP1180.11073_1, Opisthokonta.Fungi-Colletotrichum_higginsianum.XP_018152613.1, Bacteria.Betaproteobacteria-Nitrosomonas_cryotolerans.WP_028461037.1, Bacteria.Bacteroidetes-Candidatus_Amoebophilus.WP_012473465.1, Bacteria.Alphaproteobacteria-Wolbachia_endosymbiont.WP_015587727.1, Plantae.Rhodophyta-Porphyridium_aerugineum.MMETSP0313.36889_1, Opisthokonta.Fungi-Coccidioides_immitis.XP_001242411.2, Bacteria.Betaproteobacteria-Nitrosomonas_sp..WP_013967098.1, Bacteria.Firmicutes-Acetohalobium_arabaticum.WP_083771337.1, Chromalveolata.Cryptophyta-Hemiselmis_andersenii.CCMP441.2164_1, Chromalveolata.Alveolata-Chromera_velia.Cvel_12347]	[Bacteria.Cyanobacteria-Scytonema_tolypothrichoides.WP_048871221.1, Bacteria.Cyanobacteria-Tolypothrix_campylonemoides.WP_041034651.1, Bacteria.Cyanobacteria-Nostocales_cyanobacterium.WP_087543706.1"
				);
		bp.checkIngroup("xx", "Bacteria");
		System.out.println(bp.getStatus());
		bp.checkIngroup("xx", "Bacteria", "Viridi,Chromal");
		System.out.println(bp.getStatus());
		bp.checkIngroup("add.evm.model.contig44.4xx", "Bacteria");
		System.out.println(bp.getStatus());
		bp.checkIngroup("add.evm.model.contig44.4xx", "Bacteria", "Viridi,Chromal");
		System.out.println(bp.getStatus());
		bp.getMinorContamination().forEach(System.out::println);
		

	    bp = new ABipartition("Bacteria.Tenericutes-Spiroplasma_poulsonii.WP_040093330.1, add.evm.model.contig2149.8xx, Bacteria.Tenericutes-Spiroplasma_melliferum.WP_004028678.1]	[Bacteria.Cyanobacteria-Gloeobacter_kilaueensis.WP_041244918.1, Bacteria.Cyanobacteria-Phormidium_ambiguum.WP_073596395.1, Bacteria.Actinobacteria-Spirillospora_albida.WP_051712749.1, Bacteria.Chlorobi-Chlorobaculum_parvum.WP_012502011.1, Bacteria.Firmicutes-Clostridium_aceticum.WP_044826092.1, Bacteria.Alphaproteobacteria-Labrys_sp..WP_068293101.1, Bacteria.Firmicutes-Butyrivibrio_sp..WP_026506893.1, Bacteria.Bacteroidetes-Prevotella_copri.WP_006846560.1, Bacteria.Bacteroidetes-Parabacteroides_goldsteinii.WP_016212953.1, Bacteria.deltaepsilon-Desulfonatronospira_thiodismutans.WP_008869196.1, Bacteria.Alphaproteobacteria-Microvirga_lotononidis.WP_009489524.1, Bacteria.Firmicutes-Romboutsia_timonensis.WP_071121568.1, Bacteria.Chlorobi-Prosthecochloris_sp..WP_068867611.1, Bacteria.Firmicutes-Candidatus_Dorea.WP_053832736.1, Bacteria.Chloroflexi-Longilinea_arvoryzae.WP_083522546.1, Bacteria.Chloroflexi-Nitrolancea_hollandica.WP_008474654.1, Bacteria.Bacteroidetes-Bacteroides_sp..WP_072532273.1, Bacteria.Alphaproteobacteria-Jannaschia_seosinensis.WP_055662556.1, Bacteria.Verrucomicrobia-Verrucomicrobium_sp..WP_020493902.1, Bacteria.deltaepsilon-Geobacter_pelophilus.WP_085811948.1, Bacteria.Alphaproteobacteria-Methylocystis_parvus.WP_051001100.1, Bacteria.Cyanobacteria-unicellular_cyanobacterium.WP_087588934.1, Bacteria.Chlorobi-Prosthecochloris_aestuarii.WP_012505252.1, Bacteria.Alphaproteobacteria-Skermanella_aerolata.WP_084720405.1, Bacteria.Chlamydiae-Criblamydia_sequanensis.WP_041018666.1, Bacteria.Alphaproteobacteria-Erythrobacteraceae.WP_067400038.1, Bacteria.Bacteroidetes-Prevotella_ruminicola.WP_073204802.1, Bacteria.Planctomycetes-Zavarzinella_formosa.WP_020472653.1, Bacteria.deltaepsilon-Desulfovibrio_magneticus.WP_015862276.1, Bacteria.Actinobacteria-Candidatus_Blastococcus.WP_040339377.1, Bacteria.Chlamydiae-Candidatus_Protochlamydia.WP_011175239.1, Bacteria.deltaepsilon-Pelobacter_propionicus.WP_011735751.1, Bacteria.Chlorobi-Pelodictyon_phaeoclathratiforme.WP_012507767.1, Bacteria.Firmicutes-Eubacterium_hallii.WP_005345684.1, Bacteria.Chlamydiae-Parachlamydia_sp..WP_068468065.1, Chromalveolata.Haptophyceae-Exanthemachrysis_gayraliae.RCC1523.5658_1, Bacteria.Cyanobacteria-Crocosphaera_watsonii.WP_007308262.1, Bacteria.Bacteroidetes-Bacteroides_caecimuris.WP_065540321.1, Bacteria.Cyanobacteria-Leptolyngbya_sp..WP_006515968.1, Bacteria.Chlorobi-Pelodictyon_phaeoclathratiforme.WP_012508171.1, Bacteria.Firmicutes-Clostridiales.WP_008706143.1, Bacteria.Cyanobacteria-Leptolyngbya_ohadii.WP_088894993.1, Bacteria.deltaepsilon-Desulfosarcina_sp..WP_027354071.1, Bacteria.Chloroflexi-Ktedonobacter_racemifer.WP_007905232.1, Bacteria.Actinobacteria-Blastococcus_saxobsidens.WP_014376459.1, Bacteria.Kiritimatiellaeota-Kiritimatiella_glycovorans.WP_052881517.1, Bacteria.Cyanobacteria-Synechococcus_sp..WP_015146347.1, Bacteria.Verrucomicrobia-Verrucomicrobia_bacterium.WP_046299616.1, Bacteria.Firmicutes-Blautia_schinkii.WP_044942761.1, Bacteria.Bacteroidetes-Bacteroides_paurosaccharolyticus.WP_024994420.1, Bacteria.Planctomycetes-Fimbriiglobus_ruber.WP_088254535.1, Bacteria.Firmicutes-Lachnospiraceae_bacterium.WP_042735575.1, Plantae.Viridiplantae-Pyramimonas_parkeae.CCMP726.5016_1, Bacteria.Actinobacteria-Geodermatophilus_sp..WP_055768049.1, Bacteria.Actinobacteria-Blastococcus_sp..WP_029432405.1, Bacteria.Bacteroidetes-Bacteroides_ovatus.WP_074557793.1, Bacteria.Alphaproteobacteria-Octadecabacter_arcticus.WP_015497326.1, Bacteria.Synergistetes-Synergistes_jonesii.WP_051682595.1, Bacteria.Chlorobi-Prosthecochloris_sp..WP_085659970.1, Bacteria.deltaepsilon-Desulfovibrio_alaskensis.WP_011368546.1, Bacteria.Actinobacteria-Nakamurella_multipartita.WP_015747997.1, Bacteria.Actinobacteria-Geodermatophilus_nigrescens.WP_073419891.1, Bacteria.Chloroflexi-Ktedonobacter_racemifer.WP_007918855.1, Bacteria.Verrucomicrobia-Didymococcus_colitermitum.WP_052361747.1, Bacteria.Nitrospirae-Nitrospirae_bacterium.WP_085053102.1, Bacteria.Chloroflexi-Ardenticatena_maritima.WP_054492954.1, Bacteria.Chloroflexi-Chloroflexus_sp..WP_028457408.1, Bacteria.Firmicutes-Succinispira_mobilis.WP_019880565.1, Bacteria.Chlamydiae-Chlamydia_felis.WP_011457913.1, Bacteria.Firmicutes-Moorella_glycerini.WP_054936997.1, Bacteria.Alphaproteobacteria-Candidatus_Paracaedibacter.WP_032112544.1, Bacteria.Chloroflexi-Chloroflexus_islandicus.WP_066786347.1, Bacteria.deltaepsilon-Sorangium_cellulosum.WP_020740947.1, Bacteria.DeinococcusThermus-Deinococcus_gobiensis.WP_014685235.1, Bacteria.Cyanobacteria-Gloeobacter_violaceus.NP_923575.1, Bacteria.Chlorobi-Chlorobium_limicola.WP_012465902.1, Bacteria.unclassified-bacterium_JKG1.WP_026370758.1, Bacteria.Chlorobi-Chlorobium_chlorochromatii.WP_011361880.1, Bacteria.Fibrobacteres-Fibrobacter_sp..WP_073189153.1, Bacteria.Nitrospirae-Candidatus_Magnetobacterium.WP_040333382.1, Bacteria.Firmicutes-Eubacterium_rectale.WP_055061154.1, Bacteria.Chlorobi-Chlorobium_tepidum.NP_662288.1, Bacteria.Chlorobi-Chlorobium_phaeovibrioides.WP_011890507.1, Bacteria.Actinobacteria-Actinoplanes_awajinensis.WP_067698199.1, Bacteria.Actinobacteria-Dactylosporangium_aurantiacum.WP_033363900.1, Bacteria.Firmicutes-Acetonema_longum.WP_004098909.1, Bacteria.Chlamydiae-Chlamydia_gallinacea.WP_021828440.1, Bacteria.deltaepsilon-Desulfovibrio_magneticus.WP_015860557.1, Bacteria.Firmicutes-Thermoanaerobacterium_aotearoense.WP_014759504.1, Bacteria.DeinococcusThermus-Deinococcus_aquatilis.WP_019010622.1, Bacteria.Firmicutes-Blautia.WP_087266682.1, Bacteria.Chlorobi-Chlorobaculum_limnaeum.WP_069809052.1, Bacteria.Synergistetes-Cloacibacillus_sp..WP_087363151.1, Bacteria.Cyanobacteria-Synechococcus_sp..WP_015123662.1, Bacteria.Actinobacteria-Actinoplanes_utahensis.WP_043525248.1, Bacteria.Verrucomicrobia-Opitutaceae_bacterium.WP_068769973.1, Bacteria.Cyanobacteria-Microcoleus_sp..WP_015186253.1, Bacteria.Cyanobacteria-unicellular_cyanobacterium.WP_085436384.1, Bacteria.Actinobacteria-Frankia_alni.WP_011605313.1, Bacteria.Cyanobacteria-Leptolyngbya_sp..WP_075596877.1, Bacteria.Verrucomicrobia-Opitutaceae_bacterium.WP_007359021.1, Bacteria.deltaepsilon-Syntrophorhabdus_aromaticivorans.WP_038002335.1, Bacteria.Alphaproteobacteria-Skermanella_stibiiresistens.WP_037453114.1, Bacteria.Alphaproteobacteria-Azospirillum_brasilense.WP_041811590.1, Bacteria.Actinobacteria-Actinoplanes_subtropicus.WP_030443677.1, Bacteria.deltaepsilon-Geobacter_anodireducens.WP_066356937.1, Bacteria.Chlamydiae-Chlamydia_sp..WP_066482660.1, Bacteria.deltaepsilon-Desulfovibrio_piger.WP_006007000.1, Bacteria.Synergistetes-Synergistes_sp..WP_008710621.1, Bacteria.Synergistetes-Aminiphilus_circumscriptus.WP_029166622.1"
				);
		bp.checkIngroup("xx", "Bacteria");
		System.out.println(bp.getStatus());
		bp.checkIngroup("xx", "Bacteria", "Viridi,Chromal");
		System.out.println(bp.getStatus());
		bp.checkIngroup("add.evm.model.contig2149.8xx", "Bacteria");
		System.out.println(bp.getStatus());
		bp.checkIngroup("add.evm.model.contig2149.8xx", "Bacteria", "Viridi,Chromal");
		System.out.println(bp.getStatus());
		bp.getMinorContamination().forEach(System.out::println);
	}

}



