~~~
band gain list representation
  early reflections require frequency-dependent wall and air response; structure must align with materials.band_period_list
  option - selected
    complex per-band response: band_gain_list[k] = magnitude, band_phase_list[k] = phase
      length equals materials.band_count
      ordering equals materials.band_period_list
      preserves boundary-induced phase shifts
      deterministic and compatible with late-field modal phase
      required for realism
  option
    magnitude-only per-band response
      simpler
      omits boundary phase; reduces realism
  option
    single broadband scalar
      lowest cost
      discards spectral structure entirely

band gain list allocation and lifetime
  path objects must remain trivial; memory must not be allocated inside solver loops
  option - selected
    caller-owned contiguous storage
      band_gain_list and band_phase_list point into caller-managed arrays
      deterministic lifetime
      no internal allocation
      allows vectorized downstream access
  option
    per-path allocations
      expensive
      non-deterministic
      violates bottom-up composition
  option
    shared global tables
      cannot encode per-path wall chains
      insufficient for scene-dependent response

band response composition law
  per-band attenuation and phase must reflect wall reflectivity and air absorption
  option - selected
    multiplicative composition per path
      band_gain_list[k] = exp(-alpha_k * L) * product_j |R_k(w_j)|
      band_phase_list[k] = psi_k * L + sum_j phi_k(w_j)
      physically correct and stable
  option
    simplified magnitude-only law
      cheaper
      insufficient realism
  option
    per-segment storage
      redundant
      increases memory footprint

should an explicit beam based early reflection algorithm be implemented
  early reflections rely on deterministic point ray queries with embree; added abstractions must yield measurable benefit
  option - selected
    no explicit beam solver
      deterministic point rays suffice
      beam hierarchies add fanout, clipping, and state with no gain
      portals introduce implicit topology absent from geometry
      occlusion already resolved by visibility tests
  option
    full beam tracing framework
      large implementation footprint
      implicit occluder and portal management
      incompatible with bottom up accretive composition
  option
    restricted beam tracing
      lighter but still introduces nonessential primitives

should early reflections define a portal concept
  path visibility already governed by mesh and embree occlusion tests
  option - selected
    no portal concept
      duplicates mesh connectivity
      unnecessary structure without computational benefit
      complicates enumeration
  option
    user specified portals
      redundant with geometry
  option
    automatically inferred portals
      expensive and ambiguous

how should early reflections inform the late reflection configuration
  geometric early reflections and modal late reflections operate in orthogonal computational domains
  option - selected
    minimal coupling
      early reflections do not alter fdn structure
      coarse global parameters may be estimated but not required
      preserves architectural independence
  option
    strong coupling
      entangles modal solver with geometry
  option
    partial reuse of geometry
      mixes computational domains without benefit

should diffraction be included in the early reflection solver
  diffraction influences early arrivals; deterministic closed forms exist
  option - selected
    include first order deterministic utd diffraction
      physically valid behavior
      bounded cost
      deterministic
  option
    exclude diffraction
      physically incomplete
  option
    multi order diffraction
      combinatorial explosion

how are diffraction edges defined
  mesh derived inference is costly and ambiguous; deterministic input preferred
  option - selected
    user provided edges
      avoids adjacency processing
      deterministic
  option
    automatic edge extraction
      expensive
      ambiguous
  option
    heuristic edge detection
      non deterministic

what diffraction model should be used
  band dependent behavior required; materials already partition space into bands
  option - selected
    utd closed form coefficient per band
      deterministic
      analytical
      integrates with band_period_list
  option
    simplified constant diffraction gain
      unrealistic
  option
    stochastic or noise augmented diffraction
      non deterministic

what path structure should represent a diffracted arrival
  unified path representation preferred for machine first processing and later aggregation
  option - selected
    reuse existing early path structure
      order = 1
      wall_index_chain = null
      direction = edge_to_receiver unit vector
      band_gain_list and band_phase_list from utd coefficients
  option
    separate diffraction path type
      redundant
~~~

# late-field spatial model
the late kernel must encode full position dependence, not a post-gain remapping
the fdn state must possess spatial structure that couples to source and channel positions
delays require abstract coordinates to support directional coupling
spatial domain chosen as [-1,1]^d for unity across positions, channels, and state directions
unit-normalized vectors represent directions; delay length encodes path time
residues alpha_kc depend on state_excitation b(p) and state_projection c_c(p,x_c)
these must be full vectors over delay lines to influence modal shapes

## option - selected
directional state model
* assign each delay a direction s_i in [-1,1]^d with norm 1
* build b as lobe over dot(u_p,s_i)
* build c_c as lobe over dot(u_c,s_i) scaled by pan law g_c
* pros: consistent with diffuse-field physics; non-separable modal residues; smooth spatial continuity
* cons: requires per-delay direction data; eigenvector-based residues needed

## option
position-only gain model
* define g_c(p) and multiply mono late field
* pros: trivial implementation; no fdn modification
* cons: collapses spatial structure; does not affect modal excitation; discarded

## option
geometric wall mapping
* assign delay positions from real room geometry
* pros: physically interpretable
* cons: requires room model; not aligned with abstract fdn topology; unnecessarily complex for late

# why is it important that state directions encode late-field propagation directions, not room surfaces
It matters because it guarantees that the late field stays physically correct for late reverberation and does not collapse into a fake geometric early-reflection model.

The reasons are structural:

* Late reverberation is dominated by *directional diffusion*, not by the *specific positions* of room surfaces.
* After a sufficient number of reflections, the sound field loses memory of the individual walls and becomes approximately isotropic and homogeneous.
* What still matters in the late field is:

* how energy flows between directions,
* how directional components decay,
* how source and listener positions couple into those directional components.

If the delay lines carried “wall positions”, then:

* you would be forcing a geometric interpretation appropriate only for early reflections
* high-order reflections would incorrectly preserve spatial detail that does not exist in real rooms
* the modal system would inherit non-physical asymmetries
* residues could encode spurious wall-like structure instead of late-field structure
* the FDN would behave like a low-order geometric reflector bank, not a diffuser

By encoding *directions* instead of *surfaces*:

* the FDN state becomes a discretization of the diffuse wave field
  (a set of directional energy components)
* eigenmodes v_k, w_k reflect the long-term mixing dynamics, not wall layout
* b(p) and c_c(p, x_c) couple source and channels into that directional field exactly where position matters for late reverberation
* residues alpha_kc vary with direction, position, and modal structure in a physically meaningful way
* early geometry remains the job of the early-reflection algorithm, without contaminating the late stage

In short:

Using *state directions* matches the physics of the late field.
Using *state wall positions* would misrepresent the physics and corrupt the modal decomposition.

# difference between early and late reflections
early:
* finite set of low-order paths with exact times and amplitudes
* direct computational geometry (image sources, ray tracing)

late:
* dense set of high-order paths compressed into a small set of delays + mixing
* geometry only enters statistically via s_i, band filters, and mixing
* modalization gives you the exact linear response of that compressed model

# should it depend on sph-sp
## option - chosen
no. the project will be more useful if it is independent of sph-sp, which is easily achievable
