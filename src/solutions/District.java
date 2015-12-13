package solutions;
import geography.FeatureCollection;

import java.util.*;

import serialization.JSONObject;
import ui.MainFrame;

public class District extends JSONObject {
    Vector<VTD> vtds = new Vector<VTD>();
    
    public static int id = -1;
    
    double[][] outcomes;
    double[][] pop_balanced_outcomes;
    
    public static boolean use_simulated_elections = false;
    
    private double population = -1;
    
    public double area = -1;
    public double edge_length = -1;
    public double iso_quotient = -1;
    public double paired_edge_length = -1;
    public double unpaired_edge_length = -1;
    public Vector<Vector<VTD>> regions = null;

	public int excess_pop = 0;
	
	public void invalidate() {
		population = -1;
		iso_quotient = -1;
		regions = null;
		
	}
    
    void resetPopulation() {
    	population = -1;
    }

    public double getPopulation() {
    	if( population >= 0) {
    		return population;
    	}
        double pop = 0;
        for( VTD vtd : vtds) {
        	if( vtd.has_census_results) {
        		pop += vtd.population;
        	} 
        }
        population = pop;
        return pop;
    }
    public double[] getDemographics() {
    	String[] cols = MainFrame.mainframe.project.demographic_columns_as_array();
    	double[] dd = new double[cols.length];
    	if( dd.length == 0) {
    		return dd;
    	}
    	for(int i = 0; i < cols.length; i++) {
    		dd[i] = 0;
    	}
    	for( VTD vtd : vtds) {
        	for(int i = 0; i < cols.length; i++) {
        		dd[i] += vtd.demographics[i];
        	}
    	}
    	return dd;
    }
    public static double[][] getPropRepOutcome(double[] district_vote,int members_per_district) {
    	double[] prop_rep = new double[district_vote.length];
    	double[] residual_popular_vote = new double[district_vote.length];
    	double[] residual_popular_vote2 = new double[district_vote.length];
	
		double tot_vote = 0;
		for( int j = 0; j < district_vote.length; j++) {
			//popular_vote[j] += pop_district_vote[j];
			tot_vote += district_vote[j];
		}
		if( tot_vote == 0) {
			System.out.println("tot_vote = 0!");
			tot_vote = 1;
		}
		double multiplier = ((double)members_per_district) / tot_vote;
		double votes_per_seat = tot_vote / ((double)members_per_district);
		double total_votes = 0;
		for( int j = 0; j < district_vote.length; j++) {
			prop_rep[j] = multiplier*(double)district_vote[j];
			residual_popular_vote[j] = prop_rep[j] - Math.floor(prop_rep[j]);
		   	residual_popular_vote2[j] = prop_rep[j] - Math.round(prop_rep[j]);
		   	prop_rep[j] = Math.round(prop_rep[j]);
		   	total_votes += prop_rep[j];
		}
		while( total_votes < members_per_district) {
	        double max_res = 0;
	        int max_res_ind = -1;
	        for( int j = 0; j < district_vote.length; j++) {
	        	if( residual_popular_vote2[j] > max_res || max_res_ind < 0) {
	        		max_res = residual_popular_vote2[j];
	        		max_res_ind = j;
	        	}
	        }
	        residual_popular_vote2[max_res_ind] -= votes_per_seat;
	        prop_rep[max_res_ind]++;
	        total_votes++;
		}
		return new double[][]{prop_rep,residual_popular_vote,residual_popular_vote2, new double[]{votes_per_seat}};
	}
    Collection<Pair<String,Integer>> vcounties = new Vector<Pair<String,Integer>>();
    public Collection getCounties() {
    	HashMap<String,Pair<String,Integer>> counties = new HashMap<String,Pair<String,Integer>>();
    	for( VTD vtd : vtds) {
    		Pair<String,Integer> pair = counties.get(vtd.county);
    		if( pair == null) {
    			pair = new Pair<String,Integer>(vtd.county,1);
    			counties.put(vtd.county, pair);
    		} else {
    			pair.b++;
    		}
    	}
    	vcounties = counties.values();
    	return vcounties;
    }
    
    public double[][] getElectionResults() {
        double[] tot_popular_vote = new double[Settings.num_candidates];
        double[] tot_elected_vote = new double[Settings.num_candidates];
        double[] residual_popular_vote = new double[Settings.num_candidates];
        double[] residual_popular_vote2 = new double[Settings.num_candidates];
        if( outcomes == null) {
        	this.generateOutcomes(Settings.num_elections_simulated);
        }
        int result = (int)Math.floor(Math.random()*(double)outcomes.length);
        double total_pop = 0;
   	       	try {
   	            double[] district_vote = outcomes[result];
   	            double[] pop_district_vote = pop_balanced_outcomes[result];
   	            if( district_vote == null) {
   	            	int i = 0;
   	            	while( district_vote == null && i < 2) {
   	   	            	outcomes[result] = getAnOutcome();
   	   	            	district_vote = outcomes[result];
   	            		i++;
   	            	}
   	   	            if( district_vote == null) {
   	   	            }
   	   	            
   	            }
   	            
   	            double[][] prop_rep_results = getPropRepOutcome(district_vote,Settings.seats_in_district(this.id));
   	            if( prop_rep_results == null) {
   	            	System.out.println("district null prop results");
   	            }
   	            total_pop = prop_rep_results[3][0];
   	            if( total_pop == 0 || total_pop != total_pop) total_pop = 1;
		        for( int j = 0; j < district_vote.length; j++) {
		        	//System.out.println("j: "+j);
   	            	tot_elected_vote[j] += prop_rep_results[0][j];
   	            	tot_popular_vote[j] += pop_district_vote[j];
   	            	residual_popular_vote[j] += prop_rep_results[1][j];
   	            	residual_popular_vote2[j] += prop_rep_results[2][j];
   	            }
   	    	} catch (Exception ex) {
   	    		System.out.println("ex get election result "+ex);
   	    		ex.printStackTrace();
   	    	}
   	    if( Settings.ignore_uncontested && tot_popular_vote.length >=2 && FeatureCollection.buncontested1.length > id && id >= 0 && FeatureCollection.buncontested1[id]) {//(tot_popular_vote[0] == 0 || tot_popular_vote[1] == 0)) {
   	    	for( int i = 0; i < tot_popular_vote.length; i++) {
   	    		tot_popular_vote[i] = 0;
   	    	}
   	    	for( int i = 0; i < tot_elected_vote.length; i++) {
   	    		tot_elected_vote[i] = 0;
   	    	}
   	    	for( int i = 0; i < residual_popular_vote.length; i++) {
   	    		residual_popular_vote[i] = 0;
   	    	}
   	    	for( int i = 0; i < residual_popular_vote2.length; i++) {
   	    		residual_popular_vote2[i] = 0;
   	    	}
   	    }
   		
       	return new double[][]{tot_popular_vote,tot_elected_vote,residual_popular_vote,residual_popular_vote2, new double[]{total_pop}};
    }

    public double getSelfEntropy(double[] result) {
    	double tot = 0;
    	if( Settings.self_entropy_square_votecount && Settings.self_entropy_use_votecount) {
        	for( int i = 0; i < result.length; i++) {
        		result[i] *= result[i];
        	}
    	}
    	for( int i = 0; i < result.length; i++) {
    		tot += result[i];
    	}
    	for( int i = 0; i < result.length; i++) {
    		result[i] /= tot;
    	}
    	double H = 0;
    	for( int i = 0; i < result.length; i++) {
    		if( result[i] > 0) {
    			H += -result[i]*Math.log(result[i]);
    		}
    	}
    	return H;
    }
    
    /*
    public double getSelfEntropyOld(double[][] outcomes) {
    	if( outcomes == null) {
    		outcomes = generateOutcomes(Settings.num_elections_simulated);
    	}

        double total = 0;
        double[] wins  = new double[Settings.num_candidates];
        for( int i = 0; i < wins.length; i++) {
        	wins[i] = 0;
        }
    	if( Settings.use_new_self_entropy_method) {
    		double[] mu = new double[wins.length];
    		double[] sigma = new double[wins.length];
    		double[] n = new double[wins.length];
    		
    		for( ward b : wards) {
    			double[][] msn = b.getMuSigmaN();
                for( int i = 0; i < wins.length; i++) {
                	try {
            			mu[i] += msn[i][0];
            			sigma[i] += msn[i][1]; 
            			n[i] += msn[i][2];
                	} catch (Exception ex) {
                		System.out.println("i "+i+" ward "+b.id);
                		ex.printStackTrace();
                		
                	}
        		}
            }
            for( int i = 0; i < wins.length; i++) {
            	double dn = n[i]*Settings.pct_turnover;
            	mu[i] *= (1.0-Settings.pct_turnover);
            	sigma[i] *= (1.0-Settings.pct_turnover);
            	//n[i] *= (1.0-Settings.pct_turnover);
            	double dp = 0.5;
    			double dmu = dn*dp;
    			double dsigma = dn*dp*(1-dp);
            	mu[i] += dmu;
            	sigma[i] += dsigma;
            }
    		
    		double t = 0;
            for( int i = 0; i < wins.length; i++) {
            	wins[i] = Gaussian.getProbForMuSigmaN(mu[i],sigma[i],n[i]);
            	if( wins[i] != wins[i] || wins[i] <= 0) {
            		wins[i] = 0.00000001;
            	}
            	t += wins[i];
    		}
            if( t == 0) {
            	t = 1;
            }
            for( int i = 0; i < wins.length; i++) {
            	wins[i] /= t;
            }    		
    	} else {
            for( int i = 0; i < outcomes.length; i++) {
            	double[] outcome = outcomes[i];
            	if( true) {
            		double[] sim_results;
                	if( Settings.use_new_self_entropy_method) {
                		sim_results = Gaussian.getOddsFromwards(wards);
                	} else {

                    	double total_votes = 0;
                    	for( int n = 0; n < outcome.length; n++) {
                    		total_votes += outcome[n];
                    	}
                    	System.out.println("total_votes "+total_votes);
                		//double[] sim_results = StaticFunctions.convertProbsToResults(outcome, Settings.voters_to_simulate);
                		sim_results = StaticFunctions.convertProbsToResults(outcome,(int)total_votes);
                	}
                    for( int j = 0; j < outcome.length; j++) {
                    	wins[j] += sim_results[j];//outcome[j];
                    }
            	} else {
                	int best = -1;
                	double best_value = -1;
                    for( int j = 0; j < outcome.length; j++) {
                    	if( outcome[j] > best_value) {
                    		best = j;
                    		best_value = outcome[j];
                    	}
                    }
                    if( best >= 0) {
                    	wins[best]++;
                    }
            	}
            }
    	}
        for( int i = 0; i < wins.length; i++) {
            total += wins[i];
        }

        double H = 0;
        for( int i = 0; i < wins.length; i++) {
            double p = ((double)wins[i]) / total; 
            if( p != p || p <= 0) {
            	continue;
            }
            H -= p*Math.log(p);
        }
        if( H != H) {
        	H = 0;
        }

        return H;//Math.pow(H, Settings.self_entropy_exponent);
    }
    */
    
    public double[][] generateOutcomes(int num) {
    	if( Settings.adjust_vote_to_population) {
        	outcomes = new double[num][];
        	pop_balanced_outcomes = new double[num][];
        	for( int i = 0; i < num; i++) {
        		double[][] dd = getAnOutcomePair(); 
        		outcomes[i] = dd[0];
        		pop_balanced_outcomes[i] = dd[1];
        	}
        	return outcomes;
    	} else {
        	outcomes = new double[num][];
        	for( int i = 0; i < num; i++) {
        		outcomes[i] = getAnOutcome();
        	}
        	pop_balanced_outcomes = outcomes;
        	return outcomes;
    	}
    }
    
    public double[] getAnOutcome() {
        double[] district_vote = new double[Settings.num_candidates]; //inited to 0
        if( vtds.size() == 0) {
            for( int i = 0; i < district_vote.length; i++) {//most_value) {
                district_vote[i] = 0;
            }
            return district_vote;
        }
        for( VTD vtd : vtds) {
            double[] vtd_vote = vtd.getOutcome();
            if( vtd_vote != null) {
                for( int i = 0; i < vtd_vote.length; i++) {//most_value) {
                    district_vote[i] += vtd_vote[i];
                }
            }
        }
        return district_vote;
    }
    public double[][] getAnOutcomePair() {
    	try {
        double[] district_vote = new double[Settings.num_candidates]; //inited to 0
        double[] pop_district_vote = new double[Settings.num_candidates]; //inited to 0
        if( vtds.size() == 0) {
            for( int i = 0; i < district_vote.length; i++) {
                district_vote[i] = 0;
            }
            return new double[][]{district_vote,district_vote};
        }
        for( VTD ward : vtds) {
            double[] ward_vote = ward.getOutcome();
            if( ward_vote != null) {
            	double tot = 0;
                for( int i = 0; i < ward_vote.length; i++) {
                	tot += ward_vote[i];
                    district_vote[i] += ward_vote[i];
                }
                if( tot == 0) {
                	tot = 0;
                } else {
                	tot = ward.population/tot;
                }
                
                for( int i = 0; i < ward_vote.length; i++) {
                    pop_district_vote[i] += ward_vote[i]*tot;
                }
            }
        }
        return new double[][]{district_vote,pop_district_vote};
    	} catch (Exception ex) {
    		ex.printStackTrace();
    		this.generateOutcomes(Settings.num_elections_simulated);
    		return getAnOutcomePair();
    	}
    }


/*
    public double[] getVotes() {
        double[] district_vote = new double[Settings.num_candidates]; //inited to 0
        if( wards.size() == 0) {
            for( int i = 0; i < district_vote.length; i++) {//most_value) {
                district_vote[i] = 0;
            }
            return district_vote;
        }
        for( ward ward : wards) {
            double[] ward_vote = ward.getVotes();
            if( ward_vote != null) {
                for( int i = 0; i < ward_vote.length; i++) {//most_value) {
                    district_vote[i] += ward_vote[i];
                }
            }
        }
        return district_vote;
    }
    */

    //getRegionCount() counts the number of contiguous regions by counting the number of vertex cycles.  a proper map will have exactly 1 contiguous region per district.
    //this is a constraint to apply _AFTER_ a long initial optimization.  as a final tuning step.
    int getRegionCount(int[] ward_districts) {
        return getRegions(ward_districts).size();
    }

    Vector<VTD> getTopPopulationRegion(int[] ward_districts) {
        Vector<Vector<VTD>> regions = getRegions(ward_districts);
        Vector<VTD> high = null;
        double max_pop = 0;
        for( Vector<VTD> region : regions) {
            double pop = getRegionPopulation(region);
            if( pop > max_pop || high == null) {
                max_pop = pop;
                high = region;
            }
        }
        return high;
    }
    Vector<Vector<VTD>> getRegions(int[] ward_districts) {
    	/*
    	if( regions != null) {
    		return regions;
    	}*/
        Hashtable<Integer,Vector<VTD>> region_hash = new Hashtable<Integer,Vector<VTD>>();
        regions = new Vector<Vector<VTD>>();
        for( VTD ward : vtds) {
            if( region_hash.get(ward.id) != null)
                continue;
            Vector<VTD> region = new Vector<VTD>();
            regions.add(region);
            addAllConnected(ward,region,region_hash,ward_districts);
        }
        return regions;
    }
    //recursively insert connected wards.
    void addAllConnected( VTD ward, Vector<VTD> region,  Hashtable<Integer,Vector<VTD>> region_hash, int[] ward_districts) {
        if( region_hash.get(ward.id) != null)
            return;
        region.add(ward);
        region_hash.put(ward.id,region);
        for( VTD other_ward : ward.neighbors) {
        	if( ward_districts[other_ward.id] == ward_districts[ward.id]) {
        		addAllConnected( other_ward, region, region_hash, ward_districts);
        	}
        }
    }
    double getRegionPopulation(Vector<VTD> region) {
        double population = 0;
        if( region == null) {
        	return 0;
        }
        for( VTD ward : region) {
        	if( ward.has_census_results) {
        		population += ward.population;
        	}
        }
        return population;
    }

	@Override
	public void post_deserialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void pre_serialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JSONObject instantiateObject(String key) {
		// TODO Auto-generated method stub
		return null;
	}


}
