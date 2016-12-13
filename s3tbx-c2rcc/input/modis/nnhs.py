import numpy as np 
import os

class nnhs():
    log=[]
    def __init__(self,nnhs_file ):
        self.nnhs_file=nnhs_file
        try:
            fp = open(nnhs_file, 'r')
        except IOError:
            print 'cannot open:', nnhs_file 
            return
        print  'net:', os.path.basename(nnhs_file)
        z=fp.readline()
        self.problem=z
        self.input=[]
        self.invar=[]
        self.output=[]
        self.outvar=[]
        while not z.startswith('#'): 
            z=fp.readline()
            if z.startswith('input'):
                self.input.append(z)
                self.invar.append(z.split()[3])
            if z.startswith('output'):
                self.output.append(z)
                self.outvar.append(z.split()[3])
                
        self.ninp=int(fp.readline())
        self.inrange=[]
        for k in range(self.ninp):
            self.inrange.append(fp.readline().split())   #fscanf(fp,'%g',[2 self.in])
        self.inrange=np.array(self.inrange,dtype='float').transpose()    
        
        self.noutp=int(fp.readline())
        self.outrange=[]
        for k in range(self.noutp):
            self.outrange.append(fp.readline().split())    #fscanf(fp,'%g',[2 self.in])
        self.outrange=np.array(self.outrange,dtype='float').transpose()    
        
        while not '$' in z: z=fp.readline()   
        planes=fp.readline().split('=')[1].split()        #str2num(r)
        self.nplanes=int(planes[0])
        self.size=map(int, planes[1:])
        self.bias=[] #cell(1,self.nplanes-1)
        for npl in range(self.nplanes-1): #=1: self.nplanes-1
            c=fp.readline().split()  #fscanf(fp,'%s',3)
            h=np.zeros((int(c[2]))) 
            for i in range(int(c[2])):
                    h[i]=float(fp.readline())
            self.bias.append(h) #fscanf(fp,'%g',self.size(npl+1))
            
        self.wgt=[] #cell(1,self.nplanes-1)
        for npl in range(self.nplanes-1): #=1: self.nplanes-1
            c=fp.readline().split()  #fscanf(fp,'%s',3)
            h=np.zeros((int(c[3]), int(c[2])) )
            for i in range(int(c[3])):
                 for j in range(int(c[2])):
                    h[i, j]=fp.readline()
            self.wgt.append(h) #fscanf(fp,'%g',self.size(npl+1))   
        fp.close()
        self.oorange=np.zeros(self.ninp, dtype=np.int)
        
    def  ff_nnhs(self,  inp):
        act=(inp-self.inrange[0,:])/(self.inrange[1,:]-self.inrange[0,:])
        for npl in range(self.nplanes-1):
            sum=self.bias[npl]+ np.dot(self.wgt[npl], act)
#            ind=np.nonzero(sum > 10.)
#            sum[ind]=10.
#            ind=np.nonzero(sum < -10.)
#            sum[ind]=-10.
            sum[sum>10]=10.
            sum[sum<-10]=-10.
            act=1./(1.+np.exp(-sum))
        res=act*(self.outrange[1,:]-self.outrange[0,:])+self.outrange[0,:]
        return res
    
    def info(self):
        for  inp in self.input:
           print inp, 
        for outp in self.output:
            print outp, 
            
    def chk_inp(self, input):
        for i in range(self.ninp):
            if input[i]<self.inrange[0, i] or input[i]>self.inrange[1, i]:
                self.oorange[i]+=1
#                self.log.append( 'Warning: ',  self.invar[i], 'out of range!', self.inrange[0, i], '<', input[i], '>', self.inrange[1, i]
#                self.log.append( 'Warning: %s out of range!  %f< %f < %f'%(self.invar[i], self.inrange[0, i],  input[i], self.inrange[1, i]))

####################################################################### 
#test
if __name__=='__main__':
    from mpl_toolkits.mplot3d import Axes3D
    import matplotlib.pyplot as plt
    from matplotlib import cm
    
    X=np.arange(-4, 4.1, 0.16)
    Y=np.arange(-4, 4.1, 0.16)
    x, y=np.meshgrid(X,Y)
    
    nn=nnhs('/home/wschoenf/python/nn/10_0.2.net')
    nn.info()
    z=np.zeros((51, 51))
    for l in range(51):
        for m in range(51):
            inp=np.hstack([x[l, m], y[l, m]])
            z[l, m]=nn.ff_nnhs(inp)
            nn.chk_inp(inp)
            
    fig = plt.figure()
    ax = fig.add_subplot(111  ,  projection='3d')
    ax.plot_surface( x, y, z, cmap=cm.jet, rstride=1, cstride=1 )
#    ax.contour( X, Y, z, cmap=cm.jet)
    plt.show()
    #nn=nnhs('25x4x20_22.5.net')
