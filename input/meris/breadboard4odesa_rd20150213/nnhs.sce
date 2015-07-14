function res=nnhs(NN_filename)

[fp, err]=mopen(NN_filename,'r');
if(err <> 0)
    error(['nnhs: could not rd-open ' NN_filename])
end
origin=NN_filename;
problem = mgetl(fp, 1);
skip_to(fp,'#');

in=mfscanf(fp, '%d')
inrange=mfscanf(in, fp,'%g %g');
out=mfscanf(fp,'%d');
outrange=mfscanf(out, fp,'%g %g');
skip_to(fp,'$');
c=mgetl(fp, 1);
t=tokens(c, '=');
r=t(2);
planes=evstr(tokens(r));
nplanes=planes(1);
psize=planes(2:$);
for p=1:nplanes-1
	dum=mgetl(fp, 1);
	pl(p).bias=mfscanf(psize(p+1), fp, '%g ');
end
for p=1:nplanes-1
	dum=mgetl(fp, 1);
	pl(p).wgt=matrix(mfscanf(psize(p)*psize(p+1), fp, '%g '),psize(p) , psize(p+1))
end
nAlpha=10000;
alphaStart=-10.;
alphaTab=zeros(nAlpha, 1);
delta=-2.0*alphaStart/(nAlpha-1);
summ=alphaStart+0.5*delta;
for i=1:nAlpha
	alphaTab(i, 1)=1 ./(1+exp(-summ));
        summ=summ+delta;
end
recDeltaAlpha=1 ./delta;
mclose(fp);
res=tlist(['nnhs', 'origin', 'problem', 'in', 'inrange', 'out', 'outrange', 'nplanes', 'psize', 'pl' , 'nAlpha', 'alphaStart', 'alphaTab', 'recDeltaAlpha'], origin, problem, in, inrange, out, outrange, nplanes, psize, pl, nAlpha, alphaStart, alphaTab, recDeltaAlpha);
endfunction

function skip_to(fp, separator)
// skip file fp until separator is found
n=0;
while n==0
    c=mgetl(fp, 1);
    n=length(strindex(c, separator));
end
endfunction
