package org.kframework.backend.acl2;

import java.util.List;

import org.kframework.kil.Empty;
import org.kframework.kil.KList;
import org.kframework.kil.KSequence;
import org.kframework.kil.Term;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.BasicTransformer;

import com.google.common.collect.Lists;

public class ReassocKSeqTransformer extends BasicTransformer {

	public ReassocKSeqTransformer(String name, Context context) {
		super(name, context);
	}
	public ReassocKSeqTransformer(Context context) {
		this("ReassocKSeqTransformer", context);
	}
	
	@Override
	public KSequence transform(KSequence ks) {
		List<Term> ts = Lists.newArrayList();
		addSequence(ts, ks);
		return new KSequence(ts);
	}
	
	private void addSequence(List<Term> ts, KSequence ks) {
		for (Term t : ks.getContents()) {
			if (t instanceof KSequence) {
				addSequence(ts, (KSequence)t);				
			} else {
				ts.add(t);
			}			
		}		
	}
	
	@Override
	public KList transform(KList kl) {
		List<Term> ts = Lists.newArrayList();
		addKList(ts, kl);
		return new KList(ts);
	}

	private void addKList(List<Term> ts, KList ks) {
		for (Term t : ks.getContents()) {
			if (t instanceof Empty) {
			} else if (t instanceof KList) {
				addKList(ts, (KList)t);
			} else {
				ts.add(t);
			}			
		}		
	}
}
