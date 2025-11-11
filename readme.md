# sph-reverb

a deterministic, per-partial reverb with geometric early reflections and analytic late field. usable for both research and production synthesis.

a reverb effect for additive synthesis working in the frequency-domain. purely synthetic, deterministic, and fully precomputed on the level of partial parameters.

* supports arbitrary spatial layouts
* scales cleanly from instrument cavities to concert halls
* fully deterministic and reproducible

* intel embree for bounding volume hierarchy

hierarchy of algorithmic choices:
~~~
reverb
  early reflections
    geometric acoustics
      image-source method (convex, planar, specular boundaries)
      beam tracing (nonconvex or topologically complex spaces)
      uniform theory of diffraction for edge contributions
    modal correction (low-frequency only: below the Schroeder frequency or within instrument-scale cavities)
      uniform-grid helmholtz eigenproblem with robin boundary impedance; lanczos (thick-restart) for the lowest modes
  late reflections
    frequency-domain feedback delay network with eigenform and modal kernels
~~~

# late reflections
## procedures
sp_reverb_late_response_table
  phi_i(f) = exp(-j * 2π f d_i)
  G(f) = diag(phi_i(f)) * M * a(f)
  A(f) = I − G(f)
  x(f) = A(f)⁻¹ b
  h(f) = cᵀ x(f)
  gain(f) = |h(f)|
  phase(f) = arg(h(f))
  rho(f) = spectral_radius(G(f))
  tau(f) = ln(1000) / (−ln(rho(f)))

sp_reverb_late_response_lookup
  result(f_q) = linear_interp(f, gain(f), tau(f), phase(f))

sp_reverb_late_project
  channel_gain_k = gain(f_q) * dot(basis_k, projection_vector)
  channel_phase_k = phase_offset + phase(f_q)
  channel_decay_k = tau(f_q)

sp_reverb_late_eigenform
  phi_i(f) = exp(−j * 2π f d_i)
  G(f) = diag(phi_i(f)) * M * a(f)
  (λ_max(f), v_max(f)) = dominant_eigenpair(G(f))
  rho(f) = |λ_max(f)|
  tau(f) = ln(1000) / (−ln(rho(f)))
  phase(f) = arg(λ_max(f))
  gain(f) = |cᵀ v_max(f)|

sp_reverb_late_modal
  z_k = r_k * exp(j * θ_k)
  det(I − G(z_k)) = 0
  G(z_k) v_k = v_k
  w_kᵀ G(z_k) = w_kᵀ
  α_k = (cᵀ v_k)(w_kᵀ b) / (w_kᵀ v_k)
  f_k = θ_k / (2π)
  tau_k = 1 / (−ln(r_k))
  y_k(t) = |α_k| * amplitude * exp(−t / tau_k) * sin(2π f_k t + phase_offset + arg(α_k))

## functions
sp_reverb_late_response_table :: config frequency -> response_table
sp_reverb_late_response_lookup :: response_table frequency -> response
sp_reverb_late_project :: response layout -> channel_response
sp_reverb_late_eigenform :: config frequency -> response
sp_reverb_late_modal :: config -> modal_set
band_gain_at :: bands frequency -> gain
build_feedback_matrix :: delays mix gain frequency -> matrix
build_feedback_matrix_from_polar :: delays mix gain r theta -> matrix
form_identity_minus_feedback :: matrix -> matrix
lu_decompose :: matrix -> (l, u)
lu_solve :: (l, u) vector -> vector
power_iteration_dominant_eigenpair :: matrix -> (lambda, vector)
eigen_equation_value :: config r theta -> (real, imag)
eigen_equation_jacobian_finite_difference :: config r theta -> (∂Fr/∂r, ∂Fr/∂θ, ∂Fi/∂r, ∂Fi/∂θ)
newton_step_on_eigen_equation :: (r, θ) (jacobian, function) -> (r_next, θ_next)

## structures
config
  delays
  mix
  bands
  projection

response_table
  frequency
  gain
  tau
  phase

response
  gain
  decay
  phase

layout
  bases
  channel_count
  basis_length

channel_response
  gain
  phase
  decay

modal_set
  frequency
  decay
  amplitude
  phase


# early reflections
## functions
~~~
sp_reverb_early_geometry :: (vec3 ...) (index ...) -> geometry
sp_reverb_early_materials :: (frequency ...) {band -> reflectivity} {band -> attenuation} -> materials
sp_reverb_early_source :: vec3 vec3 -> source
sp_reverb_early_receiver :: vec3 vec3 -> receiver
sp_reverb_early_image :: geometry materials source receiver max_order:int path_cap:int -> config
sp_reverb_early_beam :: geometry materials source receiver max_order:int path_cap:int (index ...) -> config
sp_reverb_early_diffraction :: geometry (index ...) (frequency ...) -> config
sp_reverb_early_union :: config ... -> config
sp_reverb_early_cull :: config threshold:real max_paths:int -> config
sp_reverb_early_sine :: config sine -> sine:channel ...
sp_reverb_early_noise :: config noise -> noise:channel ...
~~~

## structures
~~~
geometry
  vertices: (vec3 ...)
  faces: (index ...)
materials
  bands: (frequency ...)
  walls: {band -> reflectivity}
  air: {band -> attenuation}
source
  position: vec3
  orientation: vec3
receiver
  position: vec3
  orientation: vec3
config
  paths: (path ...)
path
  delay: time
  gain: real
  direction: vec3
  wall_chain: (wall_index ...)
  order: integer
  band_response: (gain ...)
~~~

geometry
* defines polygonal scene surfaces
* faces are convex and planar
* all geometry is static and specified in world coordinates

materials
* defines frequency-dependent reflection and absorption for walls and air
* each wall uses a reflectivity value per frequency band
* air model defines attenuation per distance per frequency band

image
* deterministic geometric solver for convex or planar enclosures
* computes all specular reflection paths up to `max_order`
* each path stores delay, gain, direction, and band-limited attenuation

beam
* generalizes the image-source approach to nonconvex or occluded geometries
* propagates beams instead of point paths and handles portals and visibility clipping
* used when scene contains multiple rooms or nonplanar boundaries

diffraction
* adds edge diffraction paths using the uniform theory of diffraction
* edge list indexes geometry edges where diffraction is to be computed
* paths extend the specular solution to include diffracted arrivals

union
* merges multiple path sets into one
* preserves delay ordering and removes duplicates

cull
* reduces the path set by amplitude threshold or maximum count
* ensures bounded complexity while preserving dominant reflections

sine
* applies computed reflection paths to a sinusoidal partial
* per-path gain and delay modify amplitude and phase
* maps one input sine to one or more output channels depending on scene layout

noise
* same as `sine` but for broadband or stochastic excitation
* applies band-dependent reflection gains and delays
