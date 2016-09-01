Muon monitor apparatus resolution function
==========================================

The number of experimental hits for given set of pixels could be given
by the following expression:

$$H_{s,exp} = \int_{}^{}{\frac{\text{dN}}{d\Omega}f\left( \Omega \right)d\Omega},$$

where index $s$ designates specific set of pixels, $dN/d\Omega$ is the
“real” muon distribution over solid angle being investigated and
$f(\Omega)$ is the response function for given set of pixels. In simple
case this response function is one for some angles (tracks passing
through the corresponding pixels) and zero for others.

The direct calculation of $f(\Omega)$ is quite hard and instead we use a
Monte-Carlo simulation:

$$H_{s,\text{sim}} = \int_{}^{}{\left( \frac{\text{dN}}{d\Omega} \right)_{\text{sim}}f\left( \Omega \right)d\Omega}.$$

If the initial muon distribution in simulation is uniform than
$\left( \frac{\text{dN}}{d\Omega} \right)_{\text{mod}} = 1$.

Supposing real $dN/d\Omega$ does not change significantly for angles
allowed for one set of pixels, we get:

$$H_{s,exp} = \int_{}^{}{\frac{\text{dN}}{d\Omega}f\left( \Omega \right)d\Omega} \approx \frac{\text{dN}}{d\Omega}\int_{}^{}{f\left( \Omega \right)d\Omega} = N_{0}\frac{\text{dN}/N_{0}}{d\Omega}\int_{}^{}{f\left( \Omega \right)d\Omega},$$

where $N_{0}$ is total number of experimental events.

Now we can obtain real distribution:

$$\frac{dN/N_{0}}{d\Omega} = \frac{H_{s,exp}}{N_{0}\int_{}^{}{f\left( \Omega \right)d\Omega}} = \frac{H_{s,exp}N_{0,sim}}{H_{s,\text{sim}}N_{0}}\ $$
