# sph-reverb
frequency-domain reverb that scales from instrument cavities to concert halls and supports arbitrary spatial layouts.

*work in progress*

# general
states the minimal high-level specification of the system. defines stable conceptual foundations and system-wide intent. records global semantics and cross-cutting properties that anchor all other files.

## algorithm hierarchy
* early reflections
  * geometric acoustics by deterministic path enumeration
    * direct path and unbounded specular reflection chains with numeric pruning
    * visibility and occlusion by embree
  * diffraction
    * first-order uniform theory of diffraction from user-specified edges
* modal correction
  * low-frequency helmholtz eigenproblem on a uniform grid
  * robin boundary impedance
  * lanczos thick-restart eigensolver
* late reflections: modal feedback delay network in the frequency domain
  * configuration built from scene geometry, materials, and early geometric statistics
  * calibration of attenuation, mixing, and state directions from early energy, direction, and density
  * poles from the calibrated feedback matrix
  * state directions for spatial embedding of delay lines
  * source coupling by state-excitation vectors
  * channel coupling by state-projection vectors
  * residues from biorthogonal eigenvectors
  * time-domain modal synthesis per mode and channel

## system-wide assumptions
* the system is designed for offline, high-accuracy rendering and assumes no real-time constraints.
* all times and frequencies are integer sample periods.
* geometry and materials define the propagation domain.
* early, modal, and late regimes form one continuous field.

## implementation details
* platform: c17, posix.1-2008, lp64, little endian. primary target: x86_64 linux.
* dependencies: embree for early reflections; glibc due to distributed embree binaries.
* sp_time_t: unsigned integer for time, frequency, decay, and phase.
* sp_sample_t: real-valued type for gains and continuous quantities.

# system description
non-normative narrative that supports interpretation of the model.

## late reflections
late reflections model the dense modal region of the sound field using a frequency-domain feedback delay network configured from scene geometry and calibrated using early statistics.

### configuration
* delay lengths from geometric scale
* per-band attenuation from material reflectivity and air absorption
* band structure inherited from materials
* initial state_directions from representative directions
* mixing matrix chosen for diffusion and stability
* calibration updates attenuation, mixing, global gain, and state_directions to match early energy, direction, and density

### descriptive summary
* scene geometry and early statistics determine modal behavior.
* modal poles and residues characterize the late field.
* rendering yields decaying sinusoids and a residual stochastic tail.

## modal correction
modal correction defines the low-frequency field by solving the helmholtz equation with robin boundary conditions on a uniform grid. an overlap region merges helmholtz modes with fdn modes.

### descriptive summary
* helmholtz modes dominate at large periods.
* fdn modes dominate at small periods.
* the overlap ensures continuous decay and phase.

## early reflections
early reflections use direct, specular, and diffracted paths under exact visibility. geometric paths are band-independent; spectral response is introduced only when mapping through materials.

### algorithm layout
* direct path: visibility-enforced segment.
* specular reflections: mirrored-receiver constructions; visibility-enforced; order unbounded with cutoff.
* diffraction: single-edge paths under visibility constraints.
* spectral response applied only after geometric path construction.

### descriptive summary
* geometry determines admissible propagation routes.
* materials define spectral effects during mapping.
* early paths provide geometric input for early synthesis and late calibration.

# documentation
* [main](other/doc/main.md)
* [early](other/doc/early.md)
* [modal](other/doc/modal.md)
* [late](other/doc/late.md)
* [considerations](other/doc/considerations.md)

# further reading
references motivating the used physical and mathematical models.

## late reflections
* Jot R. (1991). "Etude et realisation d’un spatialisateur de sons par modeles physiques et perceptifs". Defines the FDN structure, mixing matrices, and multiband attenuation.
* Jot R., Chaigne A. (1991). "Digital delay networks for designing artificial reverberators". Establishes delay-line networks with controlled energy decay.
* Schlecht S.J., Habets E.A.P. (2015). "Reverberation modeling using a time-varying feedback delay network". Provides det(I − G(z)) = 0 and numerical root-finding of FDN poles.
* Schlecht S.J. (2019). "Feedback Delay Networks: Echo Density, Stability, and Mode Analysis". Formalizes FDN modal decomposition and biorthogonal residues.
* Schroeder M.R. (1962). "Natural sounding artificial reverberation". Introduces reverberation as sums of exponentially decaying sinusoids.
* Poletti M.A. (1996). "A modal decomposition approach to sound field reproduction". Gives projection models linking modal behavior to receiver positions.
* Daniel J., Rault J.-B., Polack J.-D. (1998). "Ambisonics encoding of the sound field associated with room acoustics". Supports late-field directional sampling.
* Siltanen S., Lokki T. (2007). "Directional analysis of room impulse responses". Demonstrates structured late-field directional energy.
* Samarasinghe P., Abhayapala T., Poletti M. (2015). "Room impulse response synthesis using 3D modal decomposition". Supports explicit source and listener modal excitation.

## modal correction
* Morse P.M., Ingard K.U. (1968). "Theoretical Acoustics". Classical reference for Helmholtz solutions and boundary conditions.
* Ihlenburg F. (1998). "Finite Element Analysis of the Helmholtz Equation". Covers numerical stability and discretization issues.
* Saad Y. (1992, 2011). "Numerical Methods for Large Eigenvalue Problems". Introduces Lanczos and thick-restart methods for large-scale eigenproblems.
* Lehoucq R.B., Sorensen D.C., Yang C. (1998). "ARPACK Users Guide". Standard reference for Lanczos-based eigenvalue solvers.
* Colton D., Kress R. (1998). "Inverse Acoustic and Electromagnetic Scattering Theory". Formalizes Helmholtz operators and boundary behavior.

## early reflections
* Krokstad J., Strøm S., Sørsdal S. (1968). "Calculating the acoustical room response by the use of a ray tracing technique". Foundational geometric-acoustics simulation with path enumeration.
* Kuttruff H. (2017). "Room Acoustics". Authoritative resource for geometric acoustics, specular reflection, and diffraction.
* Tsingos N., Funkhouser T., Naylor G. et al. (2001–2004). Works on geometric acoustic simulation and high-order reflections.
* Kouyoumjian R.G., Pathak P.H. (1974). "A Uniform Geometrical Theory of Diffraction". Classical UTD formulation for edge diffraction.
* Svensson U.P., Fred R.I., Vanderkooy J. (1999). "Analytic secondary source model of edge diffraction". Engineering-friendly model for first-order edge diffraction.
* Wald I. et al. (Embree documentation). Authoritative description of visibility algorithms and BVH traversal used by the early solver.
* Allen J., Berkley D. (1979). "Image method for efficiently simulating small-room acoustics". Classical basis for specular reflection using mirrored sources.

## general references
* Vorländer M. (2008). "Auralization". Comprehensive overview spanning early reflections, diffraction, modal behavior, and rendering.
* Kuttruff H. (2017). "Room Acoustics". (Also relevant to modal and late-field concepts.)

## wikipedia
### late reflections
* [Complex number](https://en.wikipedia.org/wiki/Complex_number)
* [Exponential decay](https://en.wikipedia.org/wiki/Exponential_decay)
* [Z-transform](https://en.wikipedia.org/wiki/Z-transform)
* [Eigenvalues and eigenvectors](https://en.wikipedia.org/wiki/Eigenvalues_and_eigenvectors)
* [Power iteration](https://en.wikipedia.org/wiki/Power_iteration)
* [Residue (complex analysis)](https://en.wikipedia.org/wiki/Residue_(complex_analysis))
* [Determinant](https://en.wikipedia.org/wiki/Determinant)
* [Jacobian matrix and determinant](https://en.wikipedia.org/wiki/Jacobian_matrix_and_determinant)
* [Newton's method](https://en.wikipedia.org/wiki/Newton%27s_method)

### modal correction
* [Helmholtz equation](https://en.wikipedia.org/wiki/Helmholtz_equation)
* [Laplacian](https://en.wikipedia.org/wiki/Laplacian)
* [Discrete Laplacian](https://en.wikipedia.org/wiki/Discrete_Laplacian)
* [Finite difference](https://en.wikipedia.org/wiki/Finite_difference)
* [Robin boundary condition](https://en.wikipedia.org/wiki/Robin_boundary_condition)
* [Eigenvalue problem](https://en.wikipedia.org/wiki/Eigenvalue_problem)
* [Lanczos algorithm](https://en.wikipedia.org/wiki/Lanczos_algorithm)
* [Hermitian matrix](https://en.wikipedia.org/wiki/Hermitian_matrix)

### early reflections
* [Geometrical acoustics](https://en.wikipedia.org/wiki/Geometrical_acoustics)
* [Specular reflection](https://en.wikipedia.org/wiki/Specular_reflection)
* [Ray tracing](https://en.wikipedia.org/wiki/Ray_tracing)
* [Visibility (geometry)](https://en.wikipedia.org/wiki/Visibility_(geometry))
* [Diffraction](https://en.wikipedia.org/wiki/Diffraction)
* [Uniform Theory of Diffraction](https://en.wikipedia.org/wiki/Uniform_theory_of_diffraction)
* [Image source method](https://en.wikipedia.org/wiki/Image_source_method)
* [Solid angle](https://en.wikipedia.org/wiki/Solid_angle)
