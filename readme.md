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
    specular geometry
      image source (convex, planar, specular)
      beam tracing (nonconvex or complex)
      optional: uniform theory of diffraction for edges
      optional: biot-tolstoy-medwin formulation for offline high precision
    modal refinement
      activate only below the schroeder frequency or inside instrument-scale cavities
  late reflections
    eigenform feedback delay network
~~~

characteristics:
* maps one partial on one channel to multiple channels. reverberation is a spatial effect
* linear, time-invariant, and material parameters are frequency-dependent but constant in time

# late reflections
models the late diffuse field of a reverberant system using an orthogonal feedback delay network.
input parameters are static; output responses are fully precomputed over frequency.
the system is linear, time-invariant, and spatial.

## functions
# precomputes a grid of frequency responses for one late-field configuration.
sp_reverb_late_table :: config (frequency ...) -> grid

# interpolates a single frequency response from the grid.
sp_reverb_late_lookup :: grid frequency -> response

# maps a single response to per-channel gains through spatial basis projection.
sp_reverb_late_project :: response layout -> (gain ...)

## structures
time: unsigned integer in samples
frequency: alias of time
gain: real scalar

response
  gain
  decay: time
  phase: time

layout
  bases: (real) ...
  channel_count: integer
  basis_length: integer

config
  delays: (time ...)
  mix: ((real ...) ...)
  bands: ((low high gain) ...)
  overall_strength: real

grid
  (frequency response) ...

## semantics

mix
* orthogonal feedback matrix of size (delay_count ** 2)
* may be hadamard for powers of two
* ensures energy-preserving diffusion
* determines global spectral and spatial coupling

bands
* define piecewise-linear frequency-dependent decay and gain shaping
* interpolated per frequency in table

delays
* define delay-line lengths of the network
* mean delay sets overall phase scale
* distribution controls modal density and echo spacing

layout
* defines spatial projection bases for multi-channel output
* per-channel basis vectors must have equal norm
* stereo uses two orthogonal or near-orthogonal bases

grid
* ordered list of {frequency -> response}
* off-grid frequencies are linearly interpolated
* recommended coverage: [0, nyquist]
* density determines spectral precision

## usage
* define config with desired delays, mix matrix, and bands
* choose frequency grid and compute responses: grid = sp_reverb_late_table(config, frequency_list)
* for each partial or band, interpolate: response = sp_reverb_late_lookup(grid, frequency)
* for each output layout, project: gains = sp_reverb_late_project(response, layout)
* apply per-channel gains, decay, and phase to synthesized sines or noise

the functions are pure; no memory allocation or side effects occur.

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
