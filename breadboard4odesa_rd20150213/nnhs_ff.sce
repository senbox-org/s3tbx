function [out, jacobi] = nnhs_ff(nn, in)
// [out, grd]=ff_nn(nn, in) evaluation of neural net response out and
// Jacobian (derivativs of output wrt. input, columns index = input index)
lhs=argn()
act=(in'-nn.inrange(:, 1))./(nn.inrange(:, 2)-nn.inrange(:, 1));
//disp(act)
if lhs==2 then
	dactdx=zeros(nn.in,nn.in);
	dadx=1 ./(nn.inrange(:, 2)-nn.inrange(:, 1));
	dactdx=diag(dadx);
end
for npl=1:nn.nplanes-1
//disp(size(nn.pl(npl).wgt'))
//disp(size(act))
	summ=nn.pl(npl).bias+nn.pl(npl).wgt'*act;
	ind=find(summ > 10.);
	summ(ind)=10.;
	ind=find(summ < -10.);
	summ(ind)=-10.;
	act=activation(nn,summ);
	//act=1 ./(1.+exp(-sum));
	if lhs==2 then 
		dactdx=diag((act.*(1-act)))*nn.pl(npl).wgt'*dactdx; 
	end
end
//disp(size(act'))
//disp(size(nn.outrange(:, 1)))
out=( act.*(nn.outrange(:, 2)-nn.outrange(:, 1))+nn.outrange(:, 1) )';
if lhs==2 then 
	jacobi=diag(nn.outrange(:, 2)-nn.outrange(:, 1))*dactdx; 
end
endfunction

function res=activation(nn, x)
index=fix((x-nn.alphaStart)*nn.recDeltaAlpha+1);
if index<1
 	index=1;
end
if index>nn.nAlpha
 	index=nn.nAlpha;
end
res=nn.alphaTab(index);
endfunction
