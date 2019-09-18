package com.dummy.myerp.model.bean.comptabilite;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class CompteComptableTest {
	
	@Test
	public void getNumeroByTest() {
		
		CompteComptable compteMichel = new CompteComptable(123, "michel");
		CompteComptable compteDidier = new CompteComptable(456, "didier");
		CompteComptable compteJean = new CompteComptable(789, "jean");
		
		CompteComptable compteTest;
		
		List<CompteComptable> list = new ArrayList<CompteComptable>();
		
		list.add(compteMichel);
		list.add(compteDidier);
		list.add(compteJean);
		
		compteTest = CompteComptable.getByNumero(list, 123);
		
		Assert.assertTrue(compteTest.getLibelle().equals("michel")
					   && compteTest.getNumero().equals(123));
	}
}