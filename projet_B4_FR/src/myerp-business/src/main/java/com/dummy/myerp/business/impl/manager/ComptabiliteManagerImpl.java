package com.dummy.myerp.business.impl.manager;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.TransactionStatus;
import com.dummy.myerp.business.contrat.manager.ComptabiliteManager;
import com.dummy.myerp.business.impl.AbstractBusinessManager;
import com.dummy.myerp.model.bean.comptabilite.CompteComptable;
import com.dummy.myerp.model.bean.comptabilite.EcritureComptable;
import com.dummy.myerp.model.bean.comptabilite.JournalComptable;
import com.dummy.myerp.model.bean.comptabilite.LigneEcritureComptable;
import com.dummy.myerp.model.bean.comptabilite.SequenceEcritureComptable;
import com.dummy.myerp.technical.exception.FunctionalException;
import com.dummy.myerp.technical.exception.NotFoundException;


/**
 * Comptabilite manager implementation.
 */
public class ComptabiliteManagerImpl extends AbstractBusinessManager implements ComptabiliteManager {

    // ==================== Attributs ====================


    // ==================== Constructeurs ====================
    /**
     * Instantiates a new Comptabilite manager.
     */
    public ComptabiliteManagerImpl() {
    }


    // ==================== Getters/Setters ====================
    @Override
    public List<CompteComptable> getListCompteComptable() {
        return getDaoProxy().getComptabiliteDao().getListCompteComptable();
    }


    @Override
    public List<JournalComptable> getListJournalComptable() {
        return getDaoProxy().getComptabiliteDao().getListJournalComptable();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EcritureComptable> getListEcritureComptable() {
        return getDaoProxy().getComptabiliteDao().getListEcritureComptable();
    }
    
    @Override
	public SequenceEcritureComptable getLastSequence(EcritureComptable pEcritureComptable) {
    	return getDaoProxy().getComptabiliteDao().getLastSequence(pEcritureComptable);
    }

    /**
     * {@inheritDoc}
     */
    // TODO à tester
    @Override
    public synchronized void addReference(EcritureComptable pEcritureComptable) {
        // TODO à implémenter
        // Bien se réferer à la JavaDoc de cette méthode !
    	
    	SequenceEcritureComptable vDerniereSequence = new SequenceEcritureComptable();
    	SequenceEcritureComptable vNouvelleSequence = new SequenceEcritureComptable();
    	
        /* Le principe :
        1.  Remonter depuis la persitance la dernière valeur de la séquence du journal pour l'année de l'écriture
            (table sequence_ecriture_comptable)*/
    	
    	vDerniereSequence = getLastSequence(pEcritureComptable);    	
    	
		/* 2.  * S'il n'y a aucun enregistrement pour le journal pour l'année concernée :
                Utiliser le numéro 1.	
                
	    	   * Sinon :
	         	Utiliser la dernière valeur + 1 	*/
    	
    	int numDerniereSequence;
    	
    	if(vDerniereSequence == null) {
    		numDerniereSequence = 1;
    	}else {
    		numDerniereSequence = vDerniereSequence.getDerniereValeur()+1;
    	}
    	
		/* 3.  Mettre à jour la référence de l'écriture avec la référence calculée (RG_Compta_5) */
    	
    	String nouvelleRef = "";
    	
    	//Extraction de l'annee
    	
    	Calendar calendar = Calendar.getInstance();
    	calendar.setTime(pEcritureComptable.getDate());
    	String annee = String.valueOf(calendar.get(Calendar.YEAR));
    	
    	//Concatenation nouvelle référence
    	
    	nouvelleRef += pEcritureComptable.getJournal().getCode() 
    				+ "-" 
    				+ annee
    				+"/"
    				+ String.format("%05d", numDerniereSequence);
    	
    	pEcritureComptable.setReference(nouvelleRef);
    	
    	//Si il n'existe pas de séquence, on la créé 
    	
    	if(vDerniereSequence == null) {
    		vNouvelleSequence.setAnnee(calendar.get(Calendar.YEAR));
    		vNouvelleSequence.setDerniereValeur(numDerniereSequence);
    		
		/* 4.  Enregistrer (insert/update) la valeur de la séquence en persitance
        (table sequence_ecriture_comptable) */
    		
    		insertSequence(vNouvelleSequence, pEcritureComptable.getJournal().getCode());
    		
    	}else {
    		vNouvelleSequence = vDerniereSequence;
    		vNouvelleSequence.setDerniereValeur(numDerniereSequence);
    		
    		try {
    			updateSequence(vNouvelleSequence, pEcritureComptable.getJournal().getCode());
    		}catch(FunctionalException fe) {
    			fe.printStackTrace();
    		}
    	}
    }

    /**
     * {@inheritDoc}
     */
    // TODO à tester
    @Override
    public void checkEcritureComptable(EcritureComptable pEcritureComptable) throws FunctionalException {
        this.checkEcritureComptableUnit(pEcritureComptable);
        this.checkEcritureComptableContext(pEcritureComptable);
    }

    /**
     * Vérifie que l'Ecriture comptable respecte les règles de gestion unitaires,
     * c'est à dire indépendemment du contexte (unicité de la référence, exercie comptable non cloturé...)
     *
     * @param pEcritureComptable -
     * @throws FunctionalException Si l'Ecriture comptable ne respecte pas les règles de gestion
     */
    // TODO tests à compléter
    protected void checkEcritureComptableUnit(EcritureComptable pEcritureComptable) throws FunctionalException {
        // ===== Vérification des contraintes unitaires sur les attributs de l'écriture
        Set<ConstraintViolation<EcritureComptable>> vViolations = getConstraintValidator().validate(pEcritureComptable);
        if (!vViolations.isEmpty()) {
            throw new FunctionalException("L'écriture comptable ne respecte pas les règles de gestion.",
                                          new ConstraintViolationException(
                                              "L'écriture comptable ne respecte pas les contraintes de validation",
                                              vViolations));
        }

        // ===== RG_Compta_2 : Pour qu'une écriture comptable soit valide, elle doit être équilibrée
        if (!pEcritureComptable.isEquilibree()) {
            throw new FunctionalException("L'écriture comptable n'est pas équilibrée.");
        }

        // ===== RG_Compta_3 : une écriture comptable doit avoir au moins 2 lignes d'écriture (1 au débit, 1 au crédit)
        int vNbrCredit = 0;
        int vNbrDebit = 0;
        for (LigneEcritureComptable vLigneEcritureComptable : pEcritureComptable.getListLigneEcriture()) {
            if (BigDecimal.ZERO.compareTo(ObjectUtils.defaultIfNull(vLigneEcritureComptable.getCredit(),
                                                                    BigDecimal.ZERO)) != 0) {
                vNbrCredit++;
            }
            if (BigDecimal.ZERO.compareTo(ObjectUtils.defaultIfNull(vLigneEcritureComptable.getDebit(),
                                                                    BigDecimal.ZERO)) != 0) {
                vNbrDebit++;
            }
        }
        // On test le nombre de lignes car si l'écriture à une seule ligne
        //      avec un montant au débit et un montant au crédit ce n'est pas valable
        if (pEcritureComptable.getListLigneEcriture().size() < 2
            || vNbrCredit < 1
            || vNbrDebit < 1) {
            throw new FunctionalException(
                "L'écriture comptable doit avoir au moins deux lignes : une ligne au débit et une ligne au crédit.");
        }

        // TODO ===== RG_Compta_5 : Format et contenu de la référence
        // vérifier que l'année dans la référence correspond bien à la date de l'écriture, idem pour le code journal...
        
        // Test année de référence = année d'écriture 
        
        //On regarde si la référence n'est pas nulle
        if(pEcritureComptable.getReference() != null) {
        	String codeRef = "";
        	String dateRef = "";
        	String reference = pEcritureComptable.getReference();
        	
        	//On extrait la date de la référence
        	dateRef = reference.substring(reference.indexOf("-"), reference.indexOf("-")+4);
        	
        	//On prend la date de l'écriture comptable
        	Calendar cal = Calendar.getInstance();
        	String dateEcriture = "";
        	cal.setTime(pEcritureComptable.getDate());
        	dateEcriture = String.valueOf(cal.get(Calendar.YEAR));
        	
        	//On compare les 2, si elles ne se valent pas, on renvoie une Exception
        	if(!dateRef.equals(dateEcriture)) {
        		throw new FunctionalException("L'année de référence ne correspond pas à l'année d'écriture de l'écriture comptable");
        	}
        	
        	//Test reference.code = journalComptable.code
        	
            //On extrait le code de la référence$
        	codeRef = reference.substring(0, reference.indexOf("-"));    
            
            //On extrait le code du journalComptable
        	String codeJournalComptable = pEcritureComptable.getJournal().getCode();
        	
        	//On compare les 2, si ils ne se valent pas, on renvoie une Exception
        	if(!codeRef.equals(codeJournalComptable)) {
        		throw new FunctionalException("Le code de la référence ne correspond pas au code du journal comptable");
        	}
        }
    }

    /**
     * Vérifie que l'Ecriture comptable respecte les règles de gestion liées au contexte
     * (unicité de la référence, année comptable non cloturé...)
     *
     * @param pEcritureComptable -
     * @throws FunctionalException Si l'Ecriture comptable ne respecte pas les règles de gestion
     */
    protected void checkEcritureComptableContext(EcritureComptable pEcritureComptable) throws FunctionalException {
        // ===== RG_Compta_6 : La référence d'une écriture comptable doit être unique
        if (StringUtils.isNoneEmpty(pEcritureComptable.getReference())) {
            try {
                // Recherche d'une écriture ayant la même référence
                EcritureComptable vECRef = getDaoProxy().getComptabiliteDao().getEcritureComptableByRef(
                    pEcritureComptable.getReference());

                // Si l'écriture à vérifier est une nouvelle écriture (id == null),
                // ou si elle ne correspond pas à l'écriture trouvée (id != idECRef),
                // c'est qu'il y a déjà une autre écriture avec la même référence
                if (pEcritureComptable.getId() == null
                    || !pEcritureComptable.getId().equals(vECRef.getId())) {
                    throw new FunctionalException("Une autre écriture comptable existe déjà avec la même référence.");
                }
            } catch (NotFoundException vEx) {
                // Dans ce cas, c'est bon, ça veut dire qu'on n'a aucune autre écriture avec la même référence.
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insertEcritureComptable(EcritureComptable pEcritureComptable) throws FunctionalException {
        this.checkEcritureComptable(pEcritureComptable);
        TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
        try {
            getDaoProxy().getComptabiliteDao().insertEcritureComptable(pEcritureComptable);
            getTransactionManager().commitMyERP(vTS);
            vTS = null;
        } finally {
            getTransactionManager().rollbackMyERP(vTS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateEcritureComptable(EcritureComptable pEcritureComptable) throws FunctionalException {
        TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
        try {
            getDaoProxy().getComptabiliteDao().updateEcritureComptable(pEcritureComptable);
            getTransactionManager().commitMyERP(vTS);
            vTS = null;
        } finally {
            getTransactionManager().rollbackMyERP(vTS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteEcritureComptable(Integer pId) {
        TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
        try {
            getDaoProxy().getComptabiliteDao().deleteEcritureComptable(pId);
            getTransactionManager().commitMyERP(vTS);
            vTS = null;
        } finally {
            getTransactionManager().rollbackMyERP(vTS);
        }
    }   
    
    
    @Override
	public void insertSequence(SequenceEcritureComptable vSequenceToInsert, String code) {
    	TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
    	try {
    		getDaoProxy().getComptabiliteDao().insertSequence(vSequenceToInsert, code);
    		getTransactionManager().commitMyERP(vTS);
    		vTS = null;
    	} finally {
    		getTransactionManager().rollbackMyERP(vTS);
    	}
    }
    
    @Override
	public void updateSequence(SequenceEcritureComptable vSequenceToUpdate, String code) throws FunctionalException {
    	TransactionStatus vTS = getTransactionManager().beginTransactionMyERP();
    	try {
    		getDaoProxy().getComptabiliteDao().updateSequence(vSequenceToUpdate, code);
    		getTransactionManager().commitMyERP(vTS);
    		vTS = null;
    	} finally {
    		getTransactionManager().rollbackMyERP(vTS);
    	}
    }
    
    
    
    
    
}
